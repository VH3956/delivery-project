package com.delivery.order.controller;

import com.delivery.order.dto.OrderRequest;
import com.delivery.order.dto.OrderResponse;
import com.delivery.order.dto.OrderStatusUpdateRequest;
import com.delivery.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Order Management API")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create a new order", description = "Customers can create a new delivery order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest request, Principal principal) {
        String customerId = principal.getName();
        OrderResponse response = orderService.createOrder(request, customerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get my orders", description = "Retrieve order history for the logged-in customer")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Principal principal) {
        String customerId = principal.getName();
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId));
    }

    @Operation(summary = "Get order details", description = "Get details of a specific order")
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable String orderId, Principal principal) {
        String customerId = principal.getName();
        return ResponseEntity.ok(orderService.getOrderById(orderId, customerId));
    }

    // ==========================================
    // SHIPPER APIs
    // ==========================================

    @Operation(summary = "Get available orders", description = "Shippers can view all unassigned orders")
    @GetMapping("/available")
    @PreAuthorize("hasRole('SHIPPER')")
    public ResponseEntity<List<OrderResponse>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrdersForShippers());
    }

    @Operation(summary = "Accept order", description = "Shipper accepts an available order")
    @PatchMapping("/{orderId}/accept")
    @PreAuthorize("hasRole('SHIPPER')")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable String orderId, Principal principal) {
        String shipperId = principal.getName();
        return ResponseEntity.ok(orderService.acceptOrder(orderId, shipperId));
    }

    @Operation(summary = "Update order status", description = "Shipper updates order status (Picked Up, In Transit, Delivered)")
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