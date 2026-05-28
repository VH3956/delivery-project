package com.delivery.user.dto;

import lombok.Data;

@Data
public class ShipperRegistrationRequest {
    private String identityCardNumber;
    private String drivingLicense;
    private String vehiclePlate;
}