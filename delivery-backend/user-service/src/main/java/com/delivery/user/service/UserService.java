package com.delivery.user.service;

import com.delivery.user.dto.ChangePasswordRequest;
import com.delivery.user.dto.UpdateProfileRequest;
import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.entity.User;
import com.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Inject the encoder

    public UserResponse createUser(UserCreationRequest request) {
        // 1. Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        // Parse the requested role
        User.Role requestedRole = User.Role.valueOf(request.getRole().toUpperCase());

        // SECURITY FIX: Block anyone from registering as an ADMIN via the public API
        if (requestedRole == User.Role.ADMIN) {
            throw new RuntimeException("Unauthorized: Cannot register as ADMIN via public API");
        }

        // 2. Map DTO to Entity and hash the password
        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                // Hash the password before saving
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(requestedRole)
                .isActive(true)
                .build();

        // 3. Save to database
        User savedUser = userRepository.save(newUser);

        // 4. Return DTO
        return UserResponse.builder()
                .id(savedUser.getId())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // 1. Get My Profile
    public UserResponse getMyProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // 2. Update Profile
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if they are provided
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        User updatedUser = userRepository.save(user);

        return getMyProfile(updatedUser.getId()); // Reuse mapping logic
    }

    // 3. Change Password
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid old password");
        }

        // Save new hashed password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Admin: Lock or Unlock a user account
    public UserResponse toggleUserStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(!user.isActive());
        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // Method specifically for Admins to create any user type (including other Admins)
    public UserResponse createUserByAdmin(UserCreationRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        User.Role requestedRole = User.Role.valueOf(request.getRole().toUpperCase());

        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(requestedRole) // Allows ADMIN, SHIPPER, or CUSTOMER
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);

        return UserResponse.builder()
                .id(savedUser.getId())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }
}