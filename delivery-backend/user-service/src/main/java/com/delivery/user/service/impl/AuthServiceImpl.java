package com.delivery.user.service.impl;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.dto.LoginRequest;
import com.delivery.user.dto.RefreshTokenRequest;
import com.delivery.user.dto.TokenResponse;
import com.delivery.user.entity.User;
import com.delivery.user.exception.BusinessException;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;
import com.delivery.user.security.JwtTokenHelper;
import com.delivery.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenHelper jwtTokenHelper;

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isVerified()) {
            throw new BusinessException(ErrorCode.INVALID_OTP);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        List<String> roles = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());

        String accessToken = jwtTokenHelper.generateAccessToken(user, roles);
        String refreshToken = jwtTokenHelper.generateRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(roles.isEmpty() ? "USER" : roles.get(0))
                .build();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtTokenHelper.isTokenValid(token)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid refresh token");
        }

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