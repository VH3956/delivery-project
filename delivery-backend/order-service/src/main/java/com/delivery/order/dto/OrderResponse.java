package com.delivery.order.dto;

import com.delivery.order.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private String id;
    private String customerId;
    private String shipperId;
    private OrderStatus status;

    private String itemName;
    private BigDecimal itemWeight;
    private BigDecimal distanceKm;

    // Original calculated fee before discount
    private BigDecimal deliveryFee;

    // Voucher information
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal finalDeliveryFee;

    // Final amount customer pays
    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    // VNPay URL (if applicable)
    private String paymentUrl;

    // Tracking history
    private List<TimelineResponse> timeline;

    @Data
    @Builder
    public static class TimelineResponse {
        private OrderStatus status;
        private String description;
        private LocalDateTime timestamp;
    }
}