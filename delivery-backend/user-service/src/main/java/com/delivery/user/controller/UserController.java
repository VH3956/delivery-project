package com.delivery.user.controller;

import com.delivery.user.dto.*;
import com.delivery.user.security.JwtTokenHelper;
import com.delivery.user.service.AddressService;
import com.delivery.user.service.TokenBlacklistService;
import com.delivery.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenHelper jwtTokenHelper;
    private final TokenBlacklistService tokenBlacklistService;

    private final AddressService addressService;

    // API: Create new user (POST http://localhost:8081/api/users)
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(response);
    }

    // API: Verify OTP (POST http://localhost:8081/api/users/verify)
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody VerifyOtpRequest request) {
        String message = userService.verifyOtp(request.getEmail(), request.getOtpCode());

        // Return as JSON: { "message": "Email verified successfully! You can now log in." }
        return ResponseEntity.ok(Map.of("message", message));
    }

    // API: Resend OTP (POST http://localhost:8081/api/users/resend-otp)
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@RequestBody ResendOtpRequest request) {
        userService.resendOtp(request.getEmail());
        
        return ResponseEntity.ok(Map.of("message", "A fresh verification code has been sent to your email."));
    }

    // Get my profile
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Principal principal) {
        // principal.getName() returns the userId we set in the JwtAuthenticationFilter
        return ResponseEntity.ok(userService.getMyProfile(principal.getName()));
    }

    // Update my profile
    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getName(), request));
    }

    // Change my password
    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(
            Principal principal,
            @RequestBody ChangePasswordRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        // 1. Change password in DB
        userService.changePassword(principal.getName(), request);

        // 2. Blacklist the current token so it can't be used again
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            java.util.Date expiration = jwtTokenHelper.extractExpiration(token);
            tokenBlacklistService.addToBlacklist(token, expiration.getTime());
        }

        return ResponseEntity.ok("Password changed successfully. Please login again.");
    }

    //addresses
    @PostMapping("/me/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            Principal principal,
            @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.addAddress(principal.getName(), request));
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<java.util.List<AddressResponse>> getMyAddresses(Principal principal) {
        return ResponseEntity.ok(addressService.getUserAddresses(principal.getName()));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<String> deleteAddress(
            Principal principal,
            @PathVariable String addressId) {
        addressService.deleteAddress(principal.getName(), addressId);
        return ResponseEntity.ok("Address deleted successfully");
    }
}