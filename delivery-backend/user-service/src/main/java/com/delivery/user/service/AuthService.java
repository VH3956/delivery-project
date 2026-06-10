package com.delivery.user.service;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.dto.LoginRequest;
import com.delivery.user.dto.RefreshTokenRequest;
import com.delivery.user.dto.TokenResponse;
import com.delivery.user.entity.User;
import com.delivery.user.exception.BusinessException;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;
import com.delivery.user.security.JwtTokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository; // Inject the new role repository!
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenHelper jwtTokenHelper; // Replaces JwtService

    public TokenResponse login(LoginRequest request) {
        // 1. Find user by phone (Throws our new BusinessException!)
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. Check if user verified
        if (!user.isVerified()) {
            throw new BusinessException(ErrorCode.INVALID_OTP); 
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 4. Fetch roles from the database mapping table
        List<String> roles = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        // 5. Generate tokens using the new helper
        String accessToken = jwtTokenHelper.generateAccessToken(user, roles);
        String refreshToken = jwtTokenHelper.generateRefreshToken(user);

        // 6. Return response
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(roles.isEmpty() ? "USER" : roles.get(0)) // Default fallback
                .build();
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        
        if (!jwtTokenHelper.isTokenValid(token)) {
            throw new RuntimeException("Invalid refresh token"); 
        }

        // The subject of our refresh token is the phone number
        String phone = jwtTokenHelper.extractClaim(token, claims -> claims.getSubject());
        
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> roles = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        String newAccessToken = jwtTokenHelper.generateAccessToken(user, roles);
        String newRefreshToken = jwtTokenHelper.generateRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .role(roles.isEmpty() ? "USER" : roles.get(0))
                .build();
    }
}