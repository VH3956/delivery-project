package com.delivery.order.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderCreatedEvent {
    private String orderId;
    private String pickupAddressId; // The Matchmaker needs this to find nearby drivers!
    private String deliveryAddressId;
    private BigDecimal deliveryFee;
}