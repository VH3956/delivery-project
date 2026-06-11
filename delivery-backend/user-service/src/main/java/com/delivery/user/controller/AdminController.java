package com.delivery.user.controller;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.model.ApiResponse;
import com.delivery.user.service.ShipperService;
import com.delivery.user.service.UserService;
import com.delivery.user.util.ResponseUtils;
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
    public ResponseEntity<ApiResponse<List<ShipperProfileResponse>>> getPendingShippers() {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.getPendingProfiles()));
    }

    // Approve a shipper profile (pass boolean as query param: ?approved=true)
    @PatchMapping("/shippers/{profileId}/approve")
    public ResponseEntity<ApiResponse<ShipperProfileResponse>> approveShipper(
            @PathVariable String profileId,
            @RequestParam boolean approved) {
        return ResponseEntity.ok(ResponseUtils.success(shipperService.approveShipperProfile(profileId, approved)));
    }

    // Lock or Unlock a user account
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable String userId) {
        return ResponseEntity.ok(ResponseUtils.success(userService.toggleUserStatus(userId)));
    }

    // Admin creates a new user (can assign ADMIN role)
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody UserCreationRequest request) {
        return ResponseEntity.ok(ResponseUtils.success(userService.createUserByAdmin(request)));
    }
}