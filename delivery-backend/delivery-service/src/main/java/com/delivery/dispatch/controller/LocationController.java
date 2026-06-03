package com.delivery.dispatch.controller;

import com.delivery.dispatch.service.LocationTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/delivery/location")
@RequiredArgsConstructor
@Tag(name = "Location Tracking", description = "Live location tracking for shippers")
public class LocationController {

    private final LocationTrackingService locationTrackingService;

    // The JSON body the phone will send us
    @Data
    public static class LocationUpdateRequest {
        private double longitude;
        private double latitude;
    }

    @Operation(summary = "Update shipper location",
            description = "Update real-time GPS location of a shipper")
    @PostMapping("/update")
    @PreAuthorize("hasRole('SHIPPER')") // Only verified Shippers can call this
    public ResponseEntity<String> updateLocation(
            @RequestBody LocationUpdateRequest request,
            Principal principal) { // Spring automatically injects the token data here!

        // Extract the real, un-fakeable ID from the Token
        String shipperId = principal.getName();

        locationTrackingService.updateDriverLocation(
                shipperId,
                request.getLongitude(),
                request.getLatitude()
        );
        return ResponseEntity.ok("Location updated for Shipper: " + shipperId);
    }
}