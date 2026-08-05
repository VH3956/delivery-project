package com.delivery.dispatch.client;

import com.delivery.dispatch.dto.AddressCoordinatesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// The "name" must match the application name of the user-service in Eureka!
@FeignClient(name = "user-service")
public interface UserServiceClient {

    // We will create this "internal" endpoint in the user-service in the next step
    @GetMapping("/api/internal/addresses/{addressId}/coordinates")
    AddressCoordinatesDto getCoordinates(@PathVariable("addressId") String addressId);

    // --- NEW: Fetch Rating ---
    @GetMapping("/api/internal/shippers/{shipperId}/rating")
    Double getShipperRating(@PathVariable("shipperId") String shipperId);

}