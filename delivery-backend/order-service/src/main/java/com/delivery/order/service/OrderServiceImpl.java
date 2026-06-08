package com.delivery.order.service;

import com.delivery.order.client.AddressCoordinatesDto;
import com.delivery.order.client.UserServiceClient;
import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.entity.Order;
import com.delivery.order.entity.OrderTimeline;
import com.delivery.order.entity.Voucher;
import com.delivery.order.enums.OrderStatus;
import com.delivery.order.event.OrderCreatedEvent;
import com.delivery.order.repository.OrderRepository;
import com.delivery.order.repository.OrderTimelineRepository;
import com.delivery.order.repository.VoucherRepository;
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
    private final VoucherRepository voucherRepository;
    private final VNPayService vnPayService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String customerId) {

        double pickupLat, pickupLng;
        double deliveryLat, deliveryLng;

        // 1. Resolve Pickup Location Safely
        if (request.getPickupAddressId() != null && !request.getPickupAddressId().isEmpty()) {
            AddressCoordinatesDto pickupCoords = userServiceClient.getCoordinates(request.getPickupAddressId());
            pickupLat = pickupCoords.getLatitude();
            pickupLng = pickupCoords.getLongitude();
        } else {
            // Null Check to prevent 500 Internal Server Error (NullPointerException)
            if (request.getPickupLat() == null || request.getPickupLng() == null) {
                throw new RuntimeException("Bad Request: Pickup coordinates are required if you do not provide a pickupAddressId.");
            }
            pickupLat = request.getPickupLat();
            pickupLng = request.getPickupLng();
        }

        // 2. Resolve Delivery Location Safely
        if (request.getDeliveryAddressId() != null && !request.getDeliveryAddressId().isEmpty()) {
            AddressCoordinatesDto deliveryCoords = userServiceClient.getCoordinates(request.getDeliveryAddressId());
            deliveryLat = deliveryCoords.getLatitude();
            deliveryLng = deliveryCoords.getLongitude();
        } else {
            if (request.getDeliveryLat() == null || request.getDeliveryLng() == null) {
                throw new RuntimeException("Bad Request: Delivery coordinates are required if you do not provide a deliveryAddressId.");
            }
            deliveryLat = request.getDeliveryLat();
            deliveryLng = request.getDeliveryLng();
        }

        // 3. Math Engine!
        BigDecimal distanceKm = distanceCalculationService.calculateDistanceInKm(
                pickupLat, pickupLng, deliveryLat, deliveryLng
        );

        // 4. Pricing Engine
        BigDecimal baseFee = new BigDecimal("15000");
        BigDecimal distanceFee = distanceKm.multiply(new BigDecimal("5000"));
        BigDecimal weightFee = request.getItemWeight().multiply(new BigDecimal("2000"));
        BigDecimal deliveryFee = baseFee.add(distanceFee).add(weightFee).setScale(0, RoundingMode.HALF_UP);

        // --- NEW VOUCHER LOGIC ---
        BigDecimal discount = BigDecimal.ZERO;
        String appliedVoucherId = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            Voucher voucher = voucherRepository.findByCodeAndIsActiveTrue(request.getVoucherCode().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Invalid or expired voucher code."));

            if (deliveryFee.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new RuntimeException("Delivery fee does not meet the minimum requirement for this voucher.");
            }

            discount = voucher.getDiscountAmount();
            appliedVoucherId = voucher.getId();
        }
        // -------------------------

        BigDecimal cod = request.getCodAmount() != null ? request.getCodAmount() : BigDecimal.ZERO;

        // Final Total = (Delivery Fee - Discount) + COD. Ensure it doesn't drop below 0!
        BigDecimal finalDeliveryFee = deliveryFee.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal total = finalDeliveryFee.add(cod);

        // 5. Create the Order
        Order newOrder = Order.builder()
                .customerId(customerId)
               .pickupAddressId(request.getPickupAddressId())
                .pickupLat(pickupLat)   // <-- SAVED
                .pickupLng(pickupLng)   // <-- SAVED
                .pickupAddressLine(request.getPickupAddressLine())
                .deliveryAddressId(request.getDeliveryAddressId())
                .deliveryLat(deliveryLat) // <-- SAVED
                .deliveryLng(deliveryLng) // <-- SAVED
                .deliveryAddressLine(request.getDeliveryAddressLine())
                .itemName(request.getItemName())
                .itemWeight(request.getItemWeight())
                .note(request.getNote())
                .distanceKm(distanceKm)
                .deliveryFee(deliveryFee) // Save the original fee
                .voucherId(appliedVoucherId) // <-- SAVE THE VOUCHER ID
                .codAmount(cod)
                .totalAmount(total)       // Save the discounted total
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        // 6. Timeline
        OrderTimeline initialTimeline = OrderTimeline.builder()
                .order(savedOrder)
                .status(OrderStatus.CREATED)
                .description("Order has been created successfully. Searching for nearby drivers.")
                .build();
        orderTimelineRepository.save(initialTimeline);

        // 7. Fire Kafka Event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .pickupAddressId(savedOrder.getPickupAddressId())
                .deliveryAddressId(savedOrder.getDeliveryAddressId())
                .deliveryFee(savedOrder.getDeliveryFee())
                .build();

        orderEventProducer.publishOrderCreatedEvent(event);

        OrderResponse response = mapToResponse(savedOrder);

        // Populate voucher details in the response
        if (appliedVoucherId != null) {
            response.setVoucherCode(request.getVoucherCode().toUpperCase());
            response.setDiscountAmount(discount);
            response.setFinalDeliveryFee(finalDeliveryFee);
        }

        // --- NEW VNPAY LOGIC ---
        // If the user requested VNPay, generate the secure URL and attach it to the response!
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            String paymentUrl = vnPayService.createPaymentUrl(savedOrder);
            response.setPaymentUrl(paymentUrl);
        }
        // -----------------------

        return response;
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