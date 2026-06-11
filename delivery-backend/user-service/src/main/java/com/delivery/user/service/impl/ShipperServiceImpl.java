package com.delivery.user.service.impl;

import com.delivery.user.constant.ErrorCode;
import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.ShipperRegistrationRequest;
import com.delivery.user.entity.ShipperProfile;
import com.delivery.user.entity.User;
import com.delivery.user.entity.UserRole;
import com.delivery.user.exception.BusinessException;
import com.delivery.user.mapper.ShipperMapper;
import com.delivery.user.repository.ShipperProfileRepository;
import com.delivery.user.repository.UserRepository;
import com.delivery.user.repository.UserRoleRepository;
import com.delivery.user.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipperServiceImpl implements ShipperService {

    private final ShipperProfileRepository shipperProfileRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ShipperMapper shipperMapper; // Inject MapStruct!

    @Override
    @Transactional
    public ShipperProfileResponse registerShipperProfile(String userId, ShipperRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        boolean isShipper = roles.stream().anyMatch(ur -> ur.getRole().getName().equals("ROLE_SHIPPER"));
        if (!isShipper) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "User does not hold shipper permissions.");
        }

        if (shipperProfileRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Shipper profile already exists for this user.");
        }

        if (shipperProfileRepository.existsByIdentityCardNumber(request.getIdentityCardNumber())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Identity Card Number is already in use.");
        }

        ShipperProfile profile = ShipperProfile.builder()
                .user(user)
                .identityCardNumber(request.getIdentityCardNumber())
                .drivingLicense(request.getDrivingLicense())
                .vehiclePlate(request.getVehiclePlate())
                .build();

        ShipperProfile savedProfile = shipperProfileRepository.save(profile);
        return shipperMapper.toDto(savedProfile); // Automated mapping
    }

    @Override
    public ShipperProfileResponse getMyShipperProfile(String userId) {
        ShipperProfile profile = shipperProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Shipper profile not found."));
        return shipperMapper.toDto(profile);
    }

    @Override
    @Transactional
    public ShipperProfileResponse toggleOnlineStatus(String userId) {
        ShipperProfile profile = shipperProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Shipper profile not found."));

        if (!profile.isApproved()) {
            throw new BusinessException(ErrorCode.ACTION_NOT_ALLOWED, "Cannot go online. Profile is pending admin approval.");
        }

        profile.setOnline(!profile.isOnline());
        ShipperProfile updatedProfile = shipperProfileRepository.save(profile);
        return shipperMapper.toDto(updatedProfile);
    }

    @Override
    public List<ShipperProfileResponse> getPendingProfiles() {
        return shipperProfileRepository.findAllByIsApprovedFalse()
                .stream()
                .map(shipperMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShipperProfileResponse approveShipperProfile(String profileId, boolean isApproved) {
        ShipperProfile profile = shipperProfileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Shipper profile not found."));

        profile.setApproved(isApproved);
        ShipperProfile updatedProfile = shipperProfileRepository.save(profile);
        return shipperMapper.toDto(updatedProfile);
    }
}