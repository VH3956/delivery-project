package com.delivery.user.controller;

import com.delivery.user.dto.ChangePasswordRequest;
import com.delivery.user.dto.UpdateProfileRequest;
import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.service.JwtService;
import com.delivery.user.service.TokenBlacklistService;
import com.delivery.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    // API: Create new user (POST http://localhost:8081/api/users)
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(response);
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
            java.util.Date expiration = jwtService.extractExpiration(token);
            tokenBlacklistService.addToBlacklist(token, expiration.getTime());
        }

        return ResponseEntity.ok("Password changed successfully. Please login again.");
    }
}