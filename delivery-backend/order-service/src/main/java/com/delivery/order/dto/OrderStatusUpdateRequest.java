package com.delivery.order.dto;

import com.delivery.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private OrderStatus status;

    private String note; // Optional context like "Traffic is heavy, running 10 mins late"

    private String photoUrl; // Optional proof of delivery when status is DELIVERED
}