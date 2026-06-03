package com.delivery.user.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AddressCoordinatesDto {
    private BigDecimal latitude;
    private BigDecimal longitude;
}