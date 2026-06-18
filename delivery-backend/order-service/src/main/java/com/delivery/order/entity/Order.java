package com.delivery.order.entity;

import com.delivery.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Will serve as the tracking code

    // References to User Service IDs (No Foreign Keys!)
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "shipper_id")
    private String shipperId; // Nullable when newly created

    @Column(name = "pickup_address_id")
    private String pickupAddressId;

    @Column(name = "delivery_address_id")
    private String deliveryAddressId;

    // Pickup coordinates + label
    @Column(name = "pickup_lat")
    private Double pickupLat;

    @Column(name = "pickup_lng")
    private Double pickupLng;

    @Column(name = "pickup_address_line")
    private String pickupAddressLine;

    // Delivery coordinates + label
    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "delivery_address_line")
    private String deliveryAddressLine;

    @Column(name = "voucher_id")
    private String voucherId; // Nullable

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    // Package Details
    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "item_weight", precision = 5, scale = 2)
    private BigDecimal itemWeight;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Financials & Delivery Metrics
    @Column(name = "distance_km", precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "cod_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal codAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount; // deliveryFee - voucher + codAmount

    // End of Life Details
    @Column(name = "delivery_photo_url")
    private String deliveryPhotoUrl;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}