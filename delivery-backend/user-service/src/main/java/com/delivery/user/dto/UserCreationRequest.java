package com.delivery.user.dto;

import lombok.Data;

@Data // Auto generate Getter, Setter by Lombok
public class UserCreationRequest {
    private String phone;
    private String email;
    private String password;
    private String fullName;
    private String role; // "CUSTOMER" or "SHIPPER"
}