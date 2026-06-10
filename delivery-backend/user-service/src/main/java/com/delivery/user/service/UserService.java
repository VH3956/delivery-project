package com.delivery.user.service;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.dto.ChangePasswordRequest;
import com.delivery.user.dto.UpdateProfileRequest;
import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.entity.User;
import com.delivery.user.entity.UserRole;
import com.delivery.user.exception.BusinessException;
import com.delivery.user.entity.Role;
import com.delivery.user.repository.RoleRepository;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder; // Inject the encoder
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    // Helper method to extract the primary role name for DTOs
    private String getPrimaryRoleName(String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) return "CUSTOMER";
        return userRoles.get(0).getRole().getName().replace("ROLE_", ""); // Strip prefix for frontend
    }

    public UserResponse createUser(UserCreationRequest request) {
        // 1. Check if phone/email already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // SECURITY FIX: Block anyone from registering as an ADMIN via the public API
        String requestedRole = "ROLE_" + request.getRole().toUpperCase();
        if (requestedRole.equals("ROLE_ADMIN")) {
            throw new RuntimeException("Unauthorized: Cannot register as ADMIN via public API");
        }

        // Fetch the role from the database instead of the Enum
        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new RuntimeException("Role " + requestedRole + " not found in database."));

        // 2. Map DTO to Entity and hash the password
        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                // Hash the password before saving
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .isVerified(false)
                .build();

        // 3. Save to database
        User savedUser = userRepository.save(newUser);

        // Save mapping to UserRole table
        userRoleRepository.save(UserRole.builder().user(savedUser).role(role).build());

        // 4. Generate 6-Digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // 5. Save to Redis with 5-minute expiration
        // Key format: "OTP:vh@example.com" -> Value: "123456"
        redisTemplate.opsForValue().set("OTP:" + newUser.getEmail(), otpCode, 5, TimeUnit.MINUTES);

        // 6. Send the Email! (In a real app, we'd use @Async to not block the response, but this is fine for now)
        emailService.sendOtpEmail(newUser.getEmail(), otpCode);

        // 7. Return DTO
        return UserResponse.builder()
                .id(savedUser.getId())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(getPrimaryRoleName(savedUser.getId()))
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public String verifyOtp(String email, String otpCode) {
        String redisKey = "OTP:" + email;
        String savedOtp = redisTemplate.opsForValue().getAndDelete(redisKey);

        // 1. Check if OTP exists or expired
        if (savedOtp == null) {
            throw new BusinessException(ErrorCode.INVALID_OTP);
        }

        // 2. Check if OTP matches
        if (!savedOtp.equals(otpCode)) {
            throw new BusinessException(ErrorCode.INVALID_OTP);
        }

        // 3. OTP is correct! Unlock the user account
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setVerified(true);

        // 4. Delete the OTP from Redis so it cannot be reused
        redisTemplate.delete(redisKey);

        return "Email verified successfully! You can now log in.";
    }

    @Transactional
    public void resendOtp(String email) {
        // 1. Verify the user exists and actually needs an OTP
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new RuntimeException("This account is already verified. You can log in directly.");
        }

        // 2. Generate a new 6-Digit OTP
        String newOtpCode = String.format("%06d", new Random().nextInt(999999));

        // 3. Overwrite the old Redis key (this automatically resets the 5-minute timer)
        redisTemplate.opsForValue().set("OTP:" + user.getEmail(), newOtpCode, 5, TimeUnit.MINUTES);

        // 4. Send the new email
        emailService.sendOtpEmail(user.getEmail(), newOtpCode);
    }

    // 1. Get My Profile
    public UserResponse getMyProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(getPrimaryRoleName(user.getId()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    // 2. Update Profile
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Update fields if they are provided
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        User updatedUser = userRepository.save(user);

        return getMyProfile(updatedUser.getId()); // Reuse mapping logic
    }

    // 3. Change Password
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        // Save new hashed password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Admin: Lock or Unlock a user account
    public UserResponse toggleUserStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setActive(!user.isActive());
        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(getPrimaryRoleName(savedUser.getId()))
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // Method specifically for Admins to create any user type (including other Admins)
    public UserResponse createUserByAdmin(UserCreationRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        String requestedRole = "ROLE_" + request.getRole().toUpperCase();
        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new RuntimeException("Role " + requestedRole + " not found in database."));

        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);

        userRoleRepository.save(UserRole.builder().user(savedUser).role(role).build());

        return UserResponse.builder()
                .id(savedUser.getId())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(getPrimaryRoleName(savedUser.getId()))
                .createdAt(savedUser.getCreatedAt())
                .build();
    }
}