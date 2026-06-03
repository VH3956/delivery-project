package com.delivery.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    @NotBlank(message = "Pickup address is required")
    private String pickupAddressId;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddressId;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotNull(message = "Item weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    private BigDecimal itemWeight;

    private String note; // Optional

    private BigDecimal codAmount; // Optional Cash on Delivery
}