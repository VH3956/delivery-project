package com.delivery.user.repository;

import com.delivery.user.entity.ShipperProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipperProfileRepository extends JpaRepository<ShipperProfile, String> {

    Optional<ShipperProfile> findByUserId(String userId);

    // Admin uses this to find all pending applications
    List<ShipperProfile> findAllByIsApprovedFalse();

    boolean existsByIdentityCardNumber(String identityCardNumber);
}