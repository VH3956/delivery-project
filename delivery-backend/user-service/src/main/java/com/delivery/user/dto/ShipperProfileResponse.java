package com.delivery.user.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ShipperProfileResponse {
    private String id;
    private String userId;
    private String identityCardNumber;
    private String drivingLicense;
    private String vehiclePlate;
    private BigDecimal rating;
    private boolean isApproved;
    private boolean isOnline;
}