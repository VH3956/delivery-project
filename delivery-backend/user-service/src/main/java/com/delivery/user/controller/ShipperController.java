package com.delivery.user.controller;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.ShipperRegistrationRequest;
import com.delivery.user.dto.ReviewRequest;
import com.delivery.user.model.ApiResponse;
import com.delivery.user.service.ShipperService;
import com.delivery.user.util.ResponseUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    // --- SHIPPER & ADMIN ENDPOINTS ---

    @PostMapping("/me/profile")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> registerProfile(
            Principal principal,
            @RequestBody ShipperRegistrationRequest request) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.registerShipperProfile(principal.getName(), request)));
    }

    @GetMapping("/me/profile")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> getMyProfile(Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.getMyShipperProfile(principal.getName())));
    }

    @PatchMapping("/me/status")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> toggleStatus(Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.toggleOnlineStatus(principal.getName())));
    }

    // --- CUSTOMER ENDPOINTS ---

    // SHIP-08: Review Shipper
    @PostMapping("/{shipperId}/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')") // ✅ Allows Customers to review Shippers
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @PathVariable String shipperId,
            @RequestBody @Valid ReviewRequest request,
            Principal principal) {
            
        shipperService.submitReview(principal.getName(), shipperId, request);
        return ResponseEntity.ok( ResponseUtils.success("Review submitted successfully"));
    }
}