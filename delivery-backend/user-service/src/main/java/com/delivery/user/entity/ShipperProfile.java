package com.delivery.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shipper_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // One User has exactly One Shipper Profile
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true, nullable = false)
    private User user;

    @Column(name = "identity_card_number", unique = true, nullable = false, length = 20)
    private String identityCardNumber; // CMND/CCCD

    @Column(name = "driving_license", nullable = false, length = 50)
    private String drivingLicense; // GPLX

    @Column(name = "vehicle_plate", nullable = false, length = 20)
    private String vehiclePlate; // Biển số xe

    @Column(precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("5.0");

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private boolean isApproved = false; // Requires Admin approval

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean isOnline = false; // Toggle for receiving orders
}