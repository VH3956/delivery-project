package com.delivery.dispatch.controller;

import com.delivery.dispatch.service.LocationTrackingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationTrackingService locationTrackingService;

    // The JSON body the phone will send us
    @Data
    public static class LocationUpdateRequest {
        private String shipperId; // In a real app, extract this from the JWT token!
        private double longitude;
        private double latitude;
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateLocation(@RequestBody LocationUpdateRequest request) {
        locationTrackingService.updateDriverLocation(
                request.getShipperId(),
                request.getLongitude(),
                request.getLatitude()
        );
        return ResponseEntity.ok("Location updated");
    }
}