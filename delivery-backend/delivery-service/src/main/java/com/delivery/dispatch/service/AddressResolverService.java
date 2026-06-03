package com.delivery.dispatch.service;

import com.delivery.dispatch.client.UserServiceClient;
import com.delivery.dispatch.dto.AddressCoordinatesDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressResolverService {

    private final UserServiceClient userServiceClient;

    public double[] getCoordinates(String addressId) {
        log.info("🔍 Resolving coordinates for Address ID: {}", addressId);

        try {
            // Make the real HTTP call to the user-service!
            AddressCoordinatesDto dto = userServiceClient.getCoordinates(addressId);

            log.info("✅ Successfully fetched coordinates from user-service: [{}, {}]",
                    dto.getLongitude(), dto.getLatitude());

            // Return in [Longitude, Latitude] format for Redis Geo
            return new double[]{dto.getLongitude(), dto.getLatitude()};

        } catch (Exception e) {
            log.error("❌ Failed to fetch coordinates for address {}: {}", addressId, e.getMessage());
            // Fallback default (Hanoi) just in case the user-service is down during testing
            return new double[]{105.8342, 21.0278};
        }
    }
}