package com.delivery.user.controller;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.ShipperRegistrationRequest;
import com.delivery.user.model.ApiResponse;
import com.delivery.user.service.ShipperService;
import com.delivery.user.util.ResponseUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/shippers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHIPPER')")
public class ShipperController {

    private final ShipperService shipperService;

    // Submit documents to become a shipper
    @PostMapping("/me/profile")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> registerProfile(
            Principal principal,
            @RequestBody ShipperRegistrationRequest request) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.registerShipperProfile(principal.getName(), request)));
    }

    // View my shipper profile and approval status
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> getMyProfile(Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.getMyShipperProfile(principal.getName())));
    }

    // Toggle Online/Offline (SHIP-03)
    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> toggleStatus(Principal principal) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.toggleOnlineStatus(principal.getName())));
    }
}