package com.delivery.user.controller;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.ShipperRegistrationRequest;
import com.delivery.user.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    // Submit documents to become a shipper
    @PostMapping("/me/profile")
    public ResponseEntity<ShipperProfileResponse> registerProfile(
            Principal principal,
            @RequestBody ShipperRegistrationRequest request) {
        return ResponseEntity.ok(shipperService.registerShipperProfile(principal.getName(), request));
    }

    // View my shipper profile and approval status
    @GetMapping("/me/profile")
    public ResponseEntity<ShipperProfileResponse> getMyProfile(Principal principal) {
        return ResponseEntity.ok(shipperService.getMyShipperProfile(principal.getName()));
    }

    // Toggle Online/Offline (SHIP-03)
    @PatchMapping("/me/status")
    public ResponseEntity<ShipperProfileResponse> toggleStatus(Principal principal) {
        return ResponseEntity.ok(shipperService.toggleOnlineStatus(principal.getName()));
    }
}