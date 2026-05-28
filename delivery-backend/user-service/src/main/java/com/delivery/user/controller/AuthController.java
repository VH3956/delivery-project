package com.delivery.user.controller;

import com.delivery.user.dto.LoginRequest;
import com.delivery.user.dto.RefreshTokenRequest;
import com.delivery.user.dto.TokenResponse;
import com.delivery.user.service.AuthService;
import com.delivery.user.service.JwtService;
import com.delivery.user.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    // API: Login (POST http://localhost:8081/api/auth/login)
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // API: Logout (POST /api/auth/logout)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Extract expiration and blacklist the token
            java.util.Date expiration = jwtService.extractExpiration(token);
            tokenBlacklistService.addToBlacklist(token, expiration.getTime());
        }

        return ResponseEntity.ok("Logged out successfully");
    }
}