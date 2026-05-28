package com.delivery.user.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddressRequest {
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isDefault;
}