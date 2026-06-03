package com.delivery.order.service;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, String customerId);
    List<OrderResponse> getCustomerOrders(String customerId);
    OrderResponse getOrderById(String orderId, String customerId);

    // --- Shipper Methods ---
    List<OrderResponse> getAvailableOrdersForShippers();
    OrderResponse acceptOrder(String orderId, String shipperId);
    OrderResponse updateOrderStatus(String orderId, String shipperId, OrderStatusUpdateRequest request);
}