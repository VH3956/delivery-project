package com.delivery.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class AdminDashboardStatsResponse {
    private long totalOrders;
    private BigDecimal totalRevenue; // Sum of delivery fees/total amounts for COMPLETED orders
    private long completedOrders;
    private long cancelledOrders;
    private Map<String, Long> ordersByStatus; // e.g., {"CREATED": 15, "IN_TRANSIT": 5}
}