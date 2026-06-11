package com.delivery.user.service;

import com.delivery.user.dto.LoginRequest;
import com.delivery.user.dto.RefreshTokenRequest;
import com.delivery.user.dto.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
}