package com.delivery.user.service;

import com.delivery.user.dto.AddressCoordinatesDto;
import com.delivery.user.dto.AddressRequest;
import com.delivery.user.dto.AddressResponse;
import com.delivery.user.entity.Address;
import com.delivery.user.entity.User;
import com.delivery.user.repository.AddressRepository;
import com.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    // Add new address
    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Address> existingAddresses = addressRepository.findAllByUserId(userId);

        // If this is the user's first address, force it to be default
        boolean isDefault = request.isDefault() || existingAddresses.isEmpty();

        // If marking as default, unset default for others
        if (isDefault) {
            unsetOtherDefaults(existingAddresses);
        }

        Address address = Address.builder()
                .user(user)
                .addressLine(request.getAddressLine())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(isDefault)
                .build();

        Address savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    // Get all addresses for user
    public List<AddressResponse> getUserAddresses(String userId) {
        return addressRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Delete address
    @Transactional
    public void deleteAddress(String userId, String addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found or unauthorized"));

        addressRepository.delete(address);

        // Optional logic: If they deleted their default address, set another one as default automatically
    }

    // Helper: Unset default flag for a list of addresses
    private void unsetOtherDefaults(List<Address> addresses) {
        for (Address addr : addresses) {
            if (addr.isDefault()) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    // Helper: Map Entity to Response DTO
    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .addressLine(address.getAddressLine())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.isDefault())
                .build();
    }

    // Add this to your AddressService.java
    public AddressCoordinatesDto getCoordinates(String addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        return AddressCoordinatesDto.builder()
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .build();
    }
}