package com.delivery.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/internal/addresses/{addressId}/coordinates")
    AddressCoordinatesDto getCoordinates(@PathVariable("addressId") String addressId);
}