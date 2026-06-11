package com.delivery.user.service.impl;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.dto.ChangePasswordRequest;
import com.delivery.user.dto.UpdateProfileRequest;
import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.entity.Role;
import com.delivery.user.entity.User;
import com.delivery.user.entity.UserRole;
import com.delivery.user.exception.BusinessException;
import com.delivery.user.mapper.UserMapper;
import com.delivery.user.repository.RoleRepository;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;
import com.delivery.user.service.EmailService;
import com.delivery.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    
    private final UserMapper userMapper; // Inject MapStruct!

    private String getPrimaryRoleName(String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) return "CUSTOMER";
        return userRoles.get(0).getRole().getName().replace("ROLE_", "");
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Phone number is already in use!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Email is already in use!");
        }

        String requestedRoleName = "ROLE_" + request.getRole().toUpperCase();
        if (requestedRoleName.equals("ROLE_ADMIN")) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Cannot register as ADMIN via public API");
        }

        Role role = roleRepository.findByName(requestedRoleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .isVerified(false)
                .build();

        User savedUser = userRepository.save(newUser);
        userRoleRepository.save(UserRole.builder().user(savedUser).role(role).build());

        String otpCode = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("OTP:" + newUser.getEmail(), otpCode, 5, TimeUnit.MINUTES);
        emailService.sendOtpEmail(newUser.getEmail(), otpCode);

        // MAPSTRUCT IN ACTION! One line of code replaces the whole builder block.
        return userMapper.toDtoWithRole(savedUser, getPrimaryRoleName(savedUser.getId()));
    }

    @Override
    public UserResponse getMyProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toDtoWithRole(user, getPrimaryRoleName(user.getId()));
    }

    @Override
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        User updatedUser = userRepository.save(user);

        return userMapper.toDtoWithRole(updatedUser, getPrimaryRoleName(updatedUser.getId()));
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public UserResponse toggleUserStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setActive(!user.isActive());
        User savedUser = userRepository.save(user);

        return userMapper.toDtoWithRole(savedUser, getPrimaryRoleName(savedUser.getId()));
    }

    @Override
    @Transactional
    public UserResponse createUserByAdmin(UserCreationRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        String requestedRoleName = "ROLE_" + request.getRole().toUpperCase();
        Role role = roleRepository.findByName(requestedRoleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .isVerified(true) 
                .build();

        User savedUser = userRepository.save(newUser);
        userRoleRepository.save(UserRole.builder().user(savedUser).role(role).build());

        return userMapper.toDtoWithRole(savedUser, getPrimaryRoleName(savedUser.getId()));
    }

    @Override
    @Transactional
    public String verifyOtp(String email, String otpCode) {
        String redisKey = "OTP:" + email;
        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        if (savedOtp == null || !savedOtp.equals(otpCode)) {
            throw new BusinessException(ErrorCode.INVALID_OTP);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setVerified(true);
        userRepository.save(user);
        redisTemplate.delete(redisKey);

        return "Email verified successfully! You can now log in.";
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isVerified()) {
            throw new RuntimeException("This account is already verified.");
        }

        String newOtpCode = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("OTP:" + user.getEmail(), newOtpCode, 5, TimeUnit.MINUTES);
        emailService.sendOtpEmail(user.getEmail(), newOtpCode);
    }
}