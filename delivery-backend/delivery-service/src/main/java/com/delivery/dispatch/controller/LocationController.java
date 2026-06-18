package com.delivery.dispatch.controller;

import com.delivery.dispatch.model.ApiResponse;
import com.delivery.dispatch.service.LocationTrackingService;
import com.delivery.dispatch.util.ResponseUtils;
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

    @Data
    public static class LocationUpdateRequest {
        private double longitude;
        private double latitude;
    }

    @Operation(summary = "Update shipper location",
            description = "Update real-time GPS location of a shipper")
    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('SHIPPER', 'ADMIN')") // Only verified Shippers and ADMINs can call this
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @RequestBody LocationUpdateRequest request,
            Principal principal) { 

        String shipperId = principal.getName();

        locationTrackingService.updateDriverLocation(
                shipperId,
                request.getLongitude(),
                request.getLatitude()
        );
        
        // Wrap the response in our standard format
        return ResponseEntity.ok(ResponseUtils.success("Location updated for Shipper: " + shipperId));
    }
}