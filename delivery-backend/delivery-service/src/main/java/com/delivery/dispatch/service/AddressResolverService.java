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

        if (addressId == null || addressId.isBlank()) {
            throw new IllegalArgumentException(
                    "Address ID cannot be null or empty");
        }

        log.info("🔍 Resolving coordinates for Address ID: {}", addressId);

        AddressCoordinatesDto dto =
                userServiceClient.getCoordinates(addressId);

        log.info(
                "✅ Successfully fetched coordinates from user-service: [{}, {}]",
                dto.getLongitude(),
                dto.getLatitude());

        // Redis GEO format: [longitude, latitude]
        return new double[]{
                dto.getLongitude(),
                dto.getLatitude()
        };
    }
}