package com.delivery.user.controller;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.service.ShipperService;
import com.delivery.user.service.UserService;
import com.delivery.user.dto.UserCreationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ShipperService shipperService;
    private final UserService userService;

    // View all pending shipper applications
    @GetMapping("/shippers/pending")
    public ResponseEntity<List<ShipperProfileResponse>> getPendingShippers() {
        return ResponseEntity.ok(shipperService.getPendingProfiles());
    }

    // Approve a shipper profile (pass boolean as query param: ?approved=true)
    @PatchMapping("/shippers/{profileId}/approve")
    public ResponseEntity<ShipperProfileResponse> approveShipper(
            @PathVariable String profileId,
            @RequestParam boolean approved) {
        return ResponseEntity.ok(shipperService.approveShipperProfile(profileId, approved));
    }

    // Lock or Unlock a user account
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable String userId) {
        return ResponseEntity.ok(userService.toggleUserStatus(userId));
    }

    // Admin creates a new user (can assign ADMIN role)
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        return ResponseEntity.ok(userService.createUserByAdmin(request));
    }
}