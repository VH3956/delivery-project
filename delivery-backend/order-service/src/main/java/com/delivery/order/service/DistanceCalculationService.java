package com.delivery.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class DistanceCalculationService {

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Calculates the distance between two GPS coordinates using the Haversine formula.
     */
    public BigDecimal calculateDistanceInKm(double startLat, double startLng, double endLat, double endLng) {
        log.info("📏 Calculating Haversine distance between [{}, {}] and [{}, {}]", startLat, startLng, endLat, endLng);

        double dLat = Math.toRadians(endLat - startLat);
        double dLng = Math.toRadians(endLng - startLng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Straight-line distance
        double straightLineDistance = EARTH_RADIUS_KM * c;

        // Multiply by 1.3 to roughly estimate the actual driving distance via roads
        double estimatedDrivingDistance = straightLineDistance * 1.3;

        log.info("✅ Estimated Driving Distance: {} km", estimatedDrivingDistance);

        return new BigDecimal(estimatedDrivingDistance).setScale(1, RoundingMode.HALF_UP);
    }
}