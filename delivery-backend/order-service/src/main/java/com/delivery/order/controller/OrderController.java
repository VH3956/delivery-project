package com.delivery.order.controller;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.model.ApiResponse;
import com.delivery.order.service.OrderService;
import com.delivery.order.util.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable String orderId, @RequestParam String reason, Principal principal) {
        orderService.cancelOrder(orderId, principal.getName(), reason);
        return ResponseEntity.ok(ResponseUtils.successMessage("Order cancelled successfully"));
    }
}