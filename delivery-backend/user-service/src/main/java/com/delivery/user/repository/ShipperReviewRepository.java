package com.delivery.user.repository;

import com.delivery.user.entity.ShipperReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipperReviewRepository extends JpaRepository<ShipperReview, String> {
    
    boolean existsByOrderId(String orderId);

    // Let MySQL calculate the average rating instantly
    @Query("SELECT AVG(r.rating) FROM ShipperReview r WHERE r.shipperId = :shipperId")
    Optional<Double> calculateAverageRatingByShipperId(String shipperId);
}