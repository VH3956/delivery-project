package com.delivery.user.service;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.ShipperRegistrationRequest;
import com.delivery.user.entity.ShipperProfile;
import com.delivery.user.entity.User;
import com.delivery.user.entity.UserRole;
import com.delivery.user.repository.ShipperProfileRepository;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipperService {

    private final ShipperProfileRepository shipperProfileRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public ShipperProfileResponse registerShipperProfile(String userId, ShipperRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        boolean isShipper = roles.stream().anyMatch(ur -> ur.getRole().getName().equals("ROLE_SHIPPER"));
        if (!isShipper) {
            throw new RuntimeException("User does not hold shipper permissions.");
        }

        if (shipperProfileRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("Shipper profile already exists for this user");
        }

        if (shipperProfileRepository.existsByIdentityCardNumber(request.getIdentityCardNumber())) {
            throw new RuntimeException("Identity Card Number is already in use");
        }

        ShipperProfile profile = ShipperProfile.builder()
                .user(user)
                .identityCardNumber(request.getIdentityCardNumber())
                .drivingLicense(request.getDrivingLicense())
                .vehiclePlate(request.getVehiclePlate())
                // isApproved and isOnline default to false via Entity builder
                .build();

        ShipperProfile savedProfile = shipperProfileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    public ShipperProfileResponse getMyShipperProfile(String userId) {
        ShipperProfile profile = shipperProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Shipper profile not found"));
        return mapToResponse(profile);
    }

    @Transactional
    public ShipperProfileResponse toggleOnlineStatus(String userId) {
        ShipperProfile profile = shipperProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Shipper profile not found"));

        if (!profile.isApproved()) {
            throw new RuntimeException("Cannot go online. Profile is pending admin approval.");
        }

        profile.setOnline(!profile.isOnline());
        ShipperProfile updatedProfile = shipperProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    public ShipperProfileResponse mapToResponse(ShipperProfile profile) {
        return ShipperProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .identityCardNumber(profile.getIdentityCardNumber())
                .drivingLicense(profile.getDrivingLicense())
                .vehiclePlate(profile.getVehiclePlate())
                .rating(profile.getRating())
                .isApproved(profile.isApproved())
                .isOnline(profile.isOnline())
                .build();
    }

    // Admin: Get all pending shipper profiles
    public java.util.List<ShipperProfileResponse> getPendingProfiles() {
        return shipperProfileRepository.findAllByIsApprovedFalse()
                .stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    // Admin: Approve or Reject a shipper profile
    @Transactional
    public ShipperProfileResponse approveShipperProfile(String profileId, boolean isApproved) {
        ShipperProfile profile = shipperProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Shipper profile not found"));

        profile.setApproved(isApproved);
        ShipperProfile updatedProfile = shipperProfileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }
}