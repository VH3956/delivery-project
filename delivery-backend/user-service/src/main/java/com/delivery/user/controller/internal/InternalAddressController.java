package com.delivery.user.controller.internal;

import com.delivery.user.dto.AddressCoordinatesDto;
import com.delivery.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/addresses")
@RequiredArgsConstructor
public class InternalAddressController {

    private final AddressService addressService;

    @GetMapping("/{addressId}/coordinates")
    public ResponseEntity<AddressCoordinatesDto> getCoordinates(@PathVariable String addressId) {
        return ResponseEntity.ok(addressService.getCoordinates(addressId));
    }
}