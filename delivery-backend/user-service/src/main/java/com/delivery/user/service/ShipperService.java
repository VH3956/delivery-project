package com.delivery.user.service;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.dto.ShipperRegistrationRequest;
import java.util.List;

public interface ShipperService {
    ShipperProfileResponse registerShipperProfile(String userId, ShipperRegistrationRequest request);
    ShipperProfileResponse getMyShipperProfile(String userId);
    ShipperProfileResponse toggleOnlineStatus(String userId);
    List<ShipperProfileResponse> getPendingProfiles();
    ShipperProfileResponse approveShipperProfile(String profileId, boolean isApproved);
}