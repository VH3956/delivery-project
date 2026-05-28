package com.delivery.user.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AddressResponse {
    private String id;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isDefault;
}