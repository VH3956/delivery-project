package com.delivery.order.controller;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    // 1. Create a new Order
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest request, Principal principal) {
        // principal.getName() securely returns the UUID from the JWT token!
        String customerId = principal.getName();
        OrderResponse response = orderService.createOrder(request, customerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. View my order history
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Principal principal) {
        String customerId = principal.getName();
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId));
    }

    // 3. View a specific order
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable String orderId, Principal principal) {
        String customerId = principal.getName();
        return ResponseEntity.ok(orderService.getOrderById(orderId, customerId));
    }

    // ==========================================
    // SHIPPER APIs
    // ==========================================

    // 4. View Job Board (All unassigned orders)
    @GetMapping("/available")
    @PreAuthorize("hasRole('SHIPPER')")
    public ResponseEntity<List<OrderResponse>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrdersForShippers());
    }

    // 5. Accept an Order
    @PatchMapping("/{orderId}/accept")
    @PreAuthorize("hasRole('SHIPPER')")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable String orderId, Principal principal) {
        String shipperId = principal.getName();
        return ResponseEntity.ok(orderService.acceptOrder(orderId, shipperId));
    }

    // 6. Update Order Status (Picked Up, In Transit, Delivered)
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('SHIPPER')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody @Valid OrderStatusUpdateRequest request,
            Principal principal) {

        String shipperId = principal.getName();
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, shipperId, request));
    }
}