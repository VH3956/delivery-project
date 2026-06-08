package com.delivery.dispatch.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor    // <-- ADD THIS FOR KAFKA/JACKSON
@AllArgsConstructor
public class OrderCreatedEvent {
    private String orderId;

    // Saved Address Flow
    private String pickupAddressId;
    private String deliveryAddressId;

    // Map Pin Flow
    private Double pickupLat;
    private Double pickupLng;

    private Double deliveryLat;
    private Double deliveryLng;

    private BigDecimal deliveryFee;
}