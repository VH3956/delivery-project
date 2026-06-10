package com.delivery.user.service;

import com.delivery.user.dto.ChangePasswordRequest;
import com.delivery.user.dto.UpdateProfileRequest;
import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.entity.User;
import com.delivery.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Inject the encoder
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    public UserResponse createUser(UserCreationRequest request) {
        // 1. Check if phone/email already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
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
                .isVerified(false)
                .build();

        // 3. Save to database
        User savedUser = userRepository.save(newUser);

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
                .role(savedUser.getRole().name())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public String verifyOtp(String email, String otpCode) {
        String redisKey = "OTP:" + email;
        String savedOtp = redisTemplate.opsForValue().getAndDelete(redisKey);

        // 1. Check if OTP exists or expired
        if (savedOtp == null) {
            throw new RuntimeException("OTP has expired or does not exist. Please register again or request a new code.");
        }

        // 2. Check if OTP matches
        if (!savedOtp.equals(otpCode)) {
            throw new RuntimeException("Invalid verification code!");
        }

        // 3. OTP is correct! Unlock the user account
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setVerified(true);

        // 4. Delete the OTP from Redis so it cannot be reused
        redisTemplate.delete(redisKey);

        return "Email verified successfully! You can now log in.";
    }

    @Transactional
    public void resendOtp(String email) {
        // 1. Verify the user exists and actually needs an OTP
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email."));

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