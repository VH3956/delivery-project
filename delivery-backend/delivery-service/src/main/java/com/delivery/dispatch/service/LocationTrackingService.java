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

import com.delivery.dispatch.client.UserServiceClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final StringRedisTemplate redisTemplate;
    private final UserServiceClient userServiceClient;
    private static final String DRIVER_GEO_KEY = "shippers:online:locations";
    private static final String DRIVER_RATING_KEY = "shippers:online:ratings";

    // 1. Shipper App calls this every 10 seconds to update their location
    public void updateDriverLocation(String shipperId, double longitude, double latitude) {
        Point location = new Point(longitude, latitude);
        redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, location, shipperId);
        
        // DAS-04: Cache the rating in Redis if we don't have it yet!
        if (Boolean.FALSE.equals(redisTemplate.opsForHash().hasKey(DRIVER_RATING_KEY, shipperId))) {
            try {
                Double rating = userServiceClient.getShipperRating(shipperId);
                redisTemplate.opsForHash().put(DRIVER_RATING_KEY, shipperId, String.valueOf(rating));
                log.info("⭐ Cached Rating for Shipper {}: {}", shipperId, rating);
            } catch (Exception e) {
                redisTemplate.opsForHash().put(DRIVER_RATING_KEY, shipperId, "5.0"); // Safe fallback
            }
        }
    }

    // 2. Shipper goes offline
    public void removeDriver(String shipperId) {
        redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, shipperId);
        redisTemplate.opsForHash().delete(DRIVER_RATING_KEY, shipperId);
        log.info("🛑 Shipper {} went offline", shipperId);
    }

    // Helper method for the Event Listener
    public double getShipperRating(String shipperId) {
        Object rating = redisTemplate.opsForHash().get(DRIVER_RATING_KEY, shipperId);
        return rating != null ? Double.parseDouble(rating.toString()) : 0.0;
    }

    // 3. Matchmaker calls this to find drivers within a 5km radius
    public List<String> findNearbyDrivers(double pickupLongitude, double pickupLatitude, double radiusKm) {
        Point pickupLocation = new Point(pickupLongitude, pickupLatitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle within = new Circle(pickupLocation, radius);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(DRIVER_GEO_KEY, within);

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(geoResult -> geoResult.getContent().getName())
                .collect(Collectors.toList());
    }

    public void blacklistDriverForOrder(String orderId, String shipperId) {
        String key = "order:blacklist:" + orderId;
        redisTemplate.opsForSet().add(key, shipperId);
        redisTemplate.expire(key, java.time.Duration.ofHours(1)); // Auto-cleanup memory after 1 hour
        log.info("🚫 Blacklisted Shipper {} for Order {}", shipperId, orderId);
    }

    public boolean isDriverBlacklistedForOrder(String orderId, String shipperId) {
        String key = "order:blacklist:" + orderId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, shipperId));
    }
}