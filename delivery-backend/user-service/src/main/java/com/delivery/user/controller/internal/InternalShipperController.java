package com.delivery.user.controller.internal;

import com.delivery.user.repository.ShipperProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/shippers") // Matches the specific domain
@RequiredArgsConstructor
public class InternalShipperController {

    private final ShipperProfileRepository shipperProfileRepository;

    @GetMapping("/{shipperId}/rating")
    public ResponseEntity<Double> getShipperRating(@PathVariable String shipperId) {
        Double rating = shipperProfileRepository.findByUserId(shipperId)
                .map(profile -> profile.getRating().doubleValue())
                .orElse(5.0); // Default to 5.0 for brand new shippers
        
        return ResponseEntity.ok(rating);
    }
}