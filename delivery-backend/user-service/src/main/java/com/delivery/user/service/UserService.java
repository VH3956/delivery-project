package com.delivery.user.service;

import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.entity.User;
import com.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Logic generate new user
    public UserResponse createUser(UserCreationRequest request) {
        // 1. if phone number existed
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }

        // 2. Map Data from DTO to Entity
        User newUser = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                // in reality: password need to be hash by Bcrypt
                // use text first to test
                .passwordHash(request.getPassword())
                .fullName(request.getFullName())
                .role(User.Role.valueOf(request.getRole().toUpperCase()))
                .isActive(true)
                .build();

        // 3. Save to db
        User savedUser = userRepository.save(newUser);

        // 4. Map Entity to DTO to return to Frontend
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