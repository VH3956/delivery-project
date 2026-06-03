package com.delivery.order.enums;

public enum OrderStatus {
    CREATED,      // Order just created by Customer
    ASSIGNED,     // System found a Shipper
    PICKED_UP,    // Shipper got the package
    IN_TRANSIT,   // Package is on the way
    DELIVERED,    // Dropped off successfully (awaiting final completion)
    COMPLETED,    // All payments/COD settled
    CANCELLED     // Order aborted
}