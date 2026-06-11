package com.delivery.user.mapper;

import com.delivery.user.dto.UserResponse;
import com.delivery.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") // Tells MapStruct to make this a Spring Bean so we can inject it!
public interface UserMapper extends BaseMapper<User, UserResponse> {

    // MapStruct will auto-map id, phone, email, fullName, and createdAt.
    // We tell it to grab the "role" from the second parameter!
    @Mapping(target = "role", source = "roleName")
    UserResponse toDtoWithRole(User entity, String roleName);
}