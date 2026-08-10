package com.delivery.order.service;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.enums.OrderStatus;

import org.springframework.data.domain.Page;
import com.delivery.order.dto.AdminDashboardStatsResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, String customerId);
    List<OrderResponse> getCustomerOrders(String customerId);
    OrderResponse getOrderById(String orderId, String customerId);
    void cancelOrder(String orderId, String customerId, String reason);

    // --- Shipper Methods ---
    List<OrderResponse> getAvailableOrdersForShippers();
    OrderResponse acceptOrder(String orderId, String shipperId);
    OrderResponse updateOrderStatus(String orderId, String shipperId, OrderStatusUpdateRequest request);
    OrderResponse dropOrder(String orderId, String shipperId, String reason);

    // Admin Methods
    Page<OrderResponse> getAllOrdersForAdmin(int page, int size, OrderStatus status);
    AdminDashboardStatsResponse getDashboardStatistics();
}