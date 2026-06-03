package com.delivery.dispatch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final StringRedisTemplate redisTemplate;
    private static final String DRIVER_GEO_KEY = "shippers:online:locations";

    // 1. Shipper App calls this every 10 seconds to update their location
    public void updateDriverLocation(String shipperId, double longitude, double latitude) {
        Point location = new Point(longitude, latitude);
        redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, location, shipperId);
        log.info("📍 Updated location for Shipper {}: [{}, {}]", shipperId, longitude, latitude);
    }

    // 2. Shipper goes offline
    public void removeDriver(String shipperId) {
        redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, shipperId);
        log.info("🛑 Shipper {} went offline", shipperId);
    }

    // 3. Matchmaker calls this to find drivers within a 5km radius
    public List<String> findNearbyDrivers(double pickupLongitude, double pickupLatitude, double radiusKm) {
        Point pickupLocation = new Point(pickupLongitude, pickupLatitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle within = new Circle(pickupLocation, radius);

        // Ask Redis for anyone inside this circle
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(DRIVER_GEO_KEY, within);

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(geoResult -> geoResult.getContent().getName())
                .collect(Collectors.toList());
    }
}