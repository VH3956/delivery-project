package com.delivery.user.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private String id;
    private String phone;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}