package com.delivery.user.controller;

import com.delivery.user.dto.UserCreationRequest;
import com.delivery.user.dto.UserResponse;
import com.delivery.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // API: Create new user (POST http://localhost:8081/api/users)
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreationRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(response);
    }
}