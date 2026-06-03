package com.delivery.dispatch.event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderCreatedEvent {
    private String orderId;
    private String pickupAddressId;
    private String deliveryAddressId;
    private BigDecimal deliveryFee;
}