package com.delivery.user.service;

import com.delivery.user.dto.LoginRequest;
import com.delivery.user.dto.RefreshTokenRequest;
import com.delivery.user.dto.TokenResponse;
import com.delivery.user.entity.User;
import com.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        // 1. Find user by phone
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("User not found with this phone number"));

        // 2. Verify password (BCrypt compares the raw password with the hashed one in DB)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        // 3. Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Return response
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }

    // Method to generate new tokens from a valid refresh token
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String reqToken = request.getRefreshToken();

        // 1. Validate the refresh token (Check signature and expiration)
        if (!jwtService.isTokenValid(reqToken)) {
            throw new RuntimeException("Invalid or expired refresh token. Please login again.");
        }

        // 2. Extract the phone number from the token
        String phone = jwtService.extractPhone(reqToken);

        // 3. Find the user in the database
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 4. Generate a brand new pair of tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // 5. Return the new tokens
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }
}