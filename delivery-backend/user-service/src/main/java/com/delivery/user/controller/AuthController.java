package com.delivery.user.controller;

import com.delivery.user.dto.LoginRequest;
import com.delivery.user.dto.RefreshTokenRequest;
import com.delivery.user.dto.TokenResponse;
import com.delivery.user.model.ApiResponse;
import com.delivery.user.security.JwtTokenHelper;
import com.delivery.user.service.AuthService;
import com.delivery.user.service.TokenBlacklistService;
import com.delivery.user.util.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenHelper jwtTokenHelper; // Replaces JwtService
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ResponseUtils.success(response)); // Wrapped in Phase 1 format!
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ResponseUtils.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Use the new helper to get expiration
            java.util.Date expiration = jwtTokenHelper.extractExpiration(token);
            tokenBlacklistService.addToBlacklist(token, expiration.getTime());
        }

        return ResponseEntity.ok(ResponseUtils.success("Logged out successfully"));
    }
}