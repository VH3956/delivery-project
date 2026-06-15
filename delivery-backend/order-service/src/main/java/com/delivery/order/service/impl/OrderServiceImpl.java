package com.delivery.order.service.impl;

import com.delivery.order.client.AddressCoordinatesDto;
import com.delivery.order.client.UserServiceClient;
import com.delivery.order.constant.ErrorCode;
import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.entity.Order;
import com.delivery.order.entity.OrderTimeline;
import com.delivery.order.entity.Voucher;
import com.delivery.order.enums.OrderStatus;
import com.delivery.order.event.OrderCreatedEvent;
import com.delivery.order.exception.BusinessException;
import com.delivery.order.repository.OrderRepository;
import com.delivery.order.repository.OrderTimelineRepository;
import com.delivery.order.repository.VoucherRepository;
import com.delivery.order.service.DistanceCalculationService;
import com.delivery.order.service.OrderEventProducer;
import com.delivery.order.service.OrderService;
import com.delivery.order.service.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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
            if (request.getPickupLat() == null || request.getPickupLng() == null) {
                throw new BusinessException(ErrorCode.ACTION_NOT_ALLOWED, "Pickup coordinates are required.");
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
                throw new BusinessException(ErrorCode.ACTION_NOT_ALLOWED, "Delivery coordinates are required.");
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

        // Voucher Logic
        BigDecimal discount = BigDecimal.ZERO;
        String appliedVoucherId = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            Voucher voucher = voucherRepository.findByCodeAndIsActiveTrue(request.getVoucherCode().toUpperCase())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND, "Invalid or expired voucher code."));

            if (deliveryFee.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new BusinessException(ErrorCode.VOUCHER_INVALID, "Delivery fee does not meet the minimum requirement for this voucher.");
            }

            discount = voucher.getDiscountAmount();
            appliedVoucherId = voucher.getId();
        }

        BigDecimal cod = request.getCodAmount() != null ? request.getCodAmount() : BigDecimal.ZERO;

        // Final Total = (Delivery Fee - Discount) + COD. Ensure it doesn't drop below 0!
        BigDecimal finalDeliveryFee = deliveryFee.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal total = finalDeliveryFee.add(cod);

        // 5. Create the Order (Using accurate entity fields)
        Order newOrder = Order.builder()
                .customerId(customerId) // Fixed mapping
                .pickupAddressId(request.getPickupAddressId())
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .pickupAddressLine(request.getPickupAddressLine()) // Fixed mapping
                .deliveryAddressId(request.getDeliveryAddressId())
                .deliveryLat(deliveryLat)
                .deliveryLng(deliveryLng)
                .deliveryAddressLine(request.getDeliveryAddressLine()) // Fixed mapping
                .itemName(request.getItemName())
                .itemWeight(request.getItemWeight())
                .note(request.getNote())
                .distanceKm(distanceKm)
                .deliveryFee(deliveryFee)
                .voucherId(appliedVoucherId)
                .codAmount(cod)
                .totalAmount(total)
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
                .pickupLat(savedOrder.getPickupLat())
                .pickupLng(savedOrder.getPickupLng())
                .deliveryLat(savedOrder.getDeliveryLat())
                .deliveryLng(savedOrder.getDeliveryLng())
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

        // VNPay Logic
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            String paymentUrl = vnPayService.createPaymentUrl(savedOrder);
            response.setPaymentUrl(paymentUrl);
        }

        return response;
    }

    @Override
    public List<OrderResponse> getCustomerOrders(String customerId) {
        return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomerId().equals(customerId)) { // Fixed mapping
            throw new BusinessException(ErrorCode.ACTION_NOT_ALLOWED, "Access Denied: This is not your order.");
        }

        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getAvailableOrdersForShippers() {
        return orderRepository.findAllByStatus(OrderStatus.CREATED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(String orderId, String shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.CREATED || order.getShipperId() != null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "Sorry, this order is no longer available.");
        }

        order.setShipperId(shipperId);
        order.setStatus(OrderStatus.ASSIGNED);
        orderRepository.save(order);

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
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!shipperId.equals(order.getShipperId())) {
            throw new BusinessException(ErrorCode.ACTION_NOT_ALLOWED, "Access Denied: You are not assigned to this delivery.");
        }

        order.setStatus(request.getStatus());
        if (request.getPhotoUrl() != null) {
            order.setDeliveryPhotoUrl(request.getPhotoUrl());
        }
        orderRepository.save(order);

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

    @Override
    @Transactional
    public void cancelOrder(String orderId, String customerId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomerId().equals(customerId)) { // Fixed mapping
            throw new BusinessException(ErrorCode.ACTION_NOT_ALLOWED, "This is not your order.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS, "Cannot cancel this order.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        orderRepository.save(order);

        OrderTimeline timeline = OrderTimeline.builder()
                .id(UUID.randomUUID().toString())
                .order(order)
                .status(OrderStatus.CANCELLED)
                .description("Order cancelled. Reason: " + reason)
                .build();
        orderTimelineRepository.save(timeline);
    }

    // Manual mapper to easily handle appending the Timeline arrays to the DTO
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
}