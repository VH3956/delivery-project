package com.delivery.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    // Option A: Saved addresses
    private String pickupAddressId;
    private String deliveryAddressId;

    // Option B: One-time addresses from map pin
    private String pickupAddressLine;
    private Double pickupLat;
    private Double pickupLng;

    private String deliveryAddressLine;
    private Double deliveryLat;
    private Double deliveryLng;

    // Item details
    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotNull(message = "Item weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    private BigDecimal itemWeight;

    private String note;

    // Financials
    private BigDecimal codAmount;

    private String voucherCode;

    // "COD" or "VNPAY"
    private String paymentMethod;
}