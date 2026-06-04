package com.delivery.order.service;

import com.delivery.order.client.AddressCoordinatesDto;
import com.delivery.order.client.UserServiceClient;
import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.entity.Order;
import com.delivery.order.entity.OrderTimeline;
import com.delivery.order.enums.OrderStatus;
import com.delivery.order.event.OrderCreatedEvent;
import com.delivery.order.repository.OrderRepository;
import com.delivery.order.repository.OrderTimelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderTimelineRepository orderTimelineRepository;
    private final OrderEventProducer orderEventProducer;
    private final UserServiceClient userServiceClient;
    private final DistanceCalculationService distanceCalculationService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String customerId) {

        // 1. Fetch Real Coordinates from User Service!
        AddressCoordinatesDto pickup = userServiceClient.getCoordinates(request.getPickupAddressId());
        AddressCoordinatesDto delivery = userServiceClient.getCoordinates(request.getDeliveryAddressId());

        // 2. Calculate the real distance using our Math engine!
        BigDecimal distanceKm = distanceCalculationService.calculateDistanceInKm(
                pickup.getLatitude(), pickup.getLongitude(),
                delivery.getLatitude(), delivery.getLongitude()
        );

        // 2. PRICING ENGINE: Calculate Delivery Fee
        // Base fee: 15,000 VND. Plus 5,000 VND per Km. Plus 2,000 VND per Kg.
        BigDecimal baseFee = new BigDecimal("15000");
        BigDecimal distanceFee = distanceKm.multiply(new BigDecimal("5000"));
        BigDecimal weightFee = request.getItemWeight().multiply(new BigDecimal("2000"));
        BigDecimal deliveryFee = baseFee.add(distanceFee).add(weightFee).setScale(0, RoundingMode.HALF_UP);

        BigDecimal cod = request.getCodAmount() != null ? request.getCodAmount() : BigDecimal.ZERO;
        BigDecimal total = deliveryFee.add(cod);

        // 3. Create the Order
        Order newOrder = Order.builder()
                .customerId(customerId)
                .pickupAddressId(request.getPickupAddressId())
                .deliveryAddressId(request.getDeliveryAddressId())
                .itemName(request.getItemName())
                .itemWeight(request.getItemWeight())
                .note(request.getNote())
                .distanceKm(distanceKm)
                .deliveryFee(deliveryFee)
                .codAmount(cod)
                .totalAmount(total)
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        // 4. Create the initial Timeline Log
        OrderTimeline initialTimeline = OrderTimeline.builder()
                .order(savedOrder)
                .status(OrderStatus.CREATED)
                .description("Order has been created successfully. Searching for nearby drivers.")
                .build();
        orderTimelineRepository.save(initialTimeline);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .pickupAddressId(savedOrder.getPickupAddressId())
                .deliveryAddressId(savedOrder.getDeliveryAddressId())
                .deliveryFee(savedOrder.getDeliveryFee())
                .build();

        orderEventProducer.publishOrderCreatedEvent(event);

        return mapToResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getCustomerOrders(String customerId) {
        List<Order> orders = orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security Check: Make sure the person requesting the order actually owns it
        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Access Denied: This is not your order.");
        }

        return mapToResponse(order);
    }

    // Helper Method to convert DB Entity to JSON DTO
    private OrderResponse mapToResponse(Order order) {
        List<OrderTimeline> timelines = orderTimelineRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId());

        List<OrderResponse.TimelineResponse> timelineResponses = timelines.stream()
                .map(t -> OrderResponse.TimelineResponse.builder()
                        .status(t.getStatus())
                        .description(t.getDescription())
                        .timestamp(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .shipperId(order.getShipperId())
                .status(order.getStatus())
                .itemName(order.getItemName())
                .itemWeight(order.getItemWeight())
                .distanceKm(order.getDistanceKm())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .timeline(timelineResponses)
                .build();
    }

    @Override
    public List<OrderResponse> getAvailableOrdersForShippers() {
        // Find all orders that don't have a driver yet
        List<Order> orders = orderRepository.findAllByStatus(OrderStatus.CREATED);
        return orders.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(String orderId, String shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Concurrency Check: Make sure another driver didn't snatch it first!
        if (order.getStatus() != OrderStatus.CREATED || order.getShipperId() != null) {
            throw new RuntimeException("Sorry, this order is no longer available.");
        }

        // Assign to this shipper and update status
        order.setShipperId(shipperId);
        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);

        // Log to timeline
        orderTimelineRepository.save(OrderTimeline.builder()
                .order(order)
                .status(OrderStatus.ASSIGNED)
                .description("Driver has accepted the order and is heading to the pickup location.")
                .build());

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, String shipperId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security Check: Only the assigned shipper can update this order!
        if (!shipperId.equals(order.getShipperId())) {
            throw new RuntimeException("Access Denied: You are not assigned to this delivery.");
        }

        order.setStatus(request.getStatus());
        if (request.getPhotoUrl() != null) {
            order.setDeliveryPhotoUrl(request.getPhotoUrl());
        }
        orderRepository.save(order);

        // Create a dynamic timeline description
        String timelineDesc = request.getNote() != null
                ? request.getNote()
                : "Order status updated to " + request.getStatus();

        orderTimelineRepository.save(OrderTimeline.builder()
                .order(order)
                .status(request.getStatus())
                .description(timelineDesc)
                .build());

        return mapToResponse(order);
    }
}