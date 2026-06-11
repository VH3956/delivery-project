package com.delivery.user.mapper;

import com.delivery.user.dto.ShipperProfileResponse;
import com.delivery.user.entity.ShipperProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

// unmappedTargetPolicy = IGNORE cleans up those warnings during the Maven build!
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShipperMapper extends BaseMapper<ShipperProfile, ShipperProfileResponse> {
    
    @Mapping(target = "userId", source = "user.id")
    // Use the exact field names defined in your DTO
    @Mapping(target = "isApproved", expression = "java(entity.isApproved())")
    @Mapping(target = "isOnline", expression = "java(entity.isOnline())")
    ShipperProfileResponse toDto(ShipperProfile entity);
}