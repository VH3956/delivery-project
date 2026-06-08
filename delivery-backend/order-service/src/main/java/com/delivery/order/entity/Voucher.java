package com.delivery.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String code; // e.g., "FREESHIP", "DISCOUNT20"

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "min_order_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderValue; // Minimum delivery fee required to use this

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}