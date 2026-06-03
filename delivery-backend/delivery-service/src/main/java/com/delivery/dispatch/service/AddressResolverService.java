package com.delivery.dispatch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AddressResolverService {

    /**
     * MOCK SERVICE:
     * In the future, this class will use Spring Cloud OpenFeign to call:
     * GET http://user-service/api/users/addresses/{addressId}
     */
    public double[] getCoordinates(String addressId) {
        log.info("🔍 Resolving coordinates for Address ID: {}", addressId);

        // Simulating the coordinates for central Hanoi (Longitude, Latitude)
        // This exactly matches where we placed our test driver earlier!
        return new double[]{105.8342, 21.0278};
    }
}