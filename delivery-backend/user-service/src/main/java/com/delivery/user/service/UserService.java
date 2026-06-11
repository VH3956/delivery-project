package com.delivery.user.service;

import com.delivery.user.dto.ChangePasswordRequest;
import com.delivery.user.dto.UpdateProfileRequest;
import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;

public interface UserService {
    UserResponse createUser(UserCreationRequest request);
    UserResponse getMyProfile(String userId);
    UserResponse updateProfile(String userId, UpdateProfileRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    UserResponse toggleUserStatus(String userId);
    UserResponse createUserByAdmin(UserCreationRequest request);
    String verifyOtp(String email, String otpCode);
    void resendOtp(String email);
}