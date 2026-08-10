package com.delivery.order.controller;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.model.ApiResponse;
import com.delivery.order.service.OrderService;
import com.delivery.order.util.ResponseUtils;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import com.delivery.order.dto.AdminDashboardStatsResponse;
import com.delivery.order.enums.OrderStatus;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody OrderRequest request, Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(orderService.createOrder(request, principal.getName())));
    }

    @GetMapping("/user/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(orderService.getCustomerOrders(principal.getName())));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String orderId, Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(orderService.getOrderById(orderId, principal.getName())));
    }

    // Shipper Endpoints
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAvailableOrders() {
         return ResponseEntity.ok(ResponseUtils.success(orderService.getAvailableOrdersForShippers()));
    }

    @PatchMapping("/{orderId}/accept")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(@PathVariable String orderId, Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(orderService.acceptOrder(orderId, principal.getName())));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody OrderStatusUpdateRequest request,
            Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(orderService.updateOrderStatus(orderId, principal.getName(), request)));
    }

    @Operation(summary = "Drop order", description = "Shipper drops an accepted order, returning it to the pool for re-assignment")
    @PostMapping("/{orderId}/drop")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> dropOrder(
            @PathVariable String orderId,
            @RequestParam String reason,
            Principal principal) {
            
        return ResponseEntity.ok(ResponseUtils.success(orderService.dropOrder(orderId, principal.getName(), reason)));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable String orderId, @RequestParam String reason, Principal principal) {
        orderService.cancelOrder(orderId, principal.getName(), reason);
        return ResponseEntity.ok(ResponseUtils.successMessage("Order cancelled successfully"));
    }

    // ==========================================
    // ADMIN ENDPOINTS (ADM-04 & ADM-06)
    // ==========================================

    @Operation(summary = "Get all orders (Admin)", description = "ADM-04: View all orders with pagination and optional status filter")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrdersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
            
        return ResponseEntity.ok(ResponseUtils.success(orderService.getAllOrdersForAdmin(page, size, status)));
    }

    @Operation(summary = "Get Dashboard Stats", description = "ADM-06: Revenue and order statistics for Admin Dashboard")
    @GetMapping("/admin/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ResponseUtils.success(orderService.getDashboardStatistics()));
    }
}