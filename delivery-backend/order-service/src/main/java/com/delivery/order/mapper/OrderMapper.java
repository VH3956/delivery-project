package com.delivery.order.mapper;

import com.delivery.order.dto.OrderResponse;
import com.delivery.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

// unmappedTargetPolicy = IGNORE keeps the build logs clean of warnings
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper extends BaseMapper<Order, OrderResponse> {
    // MapStruct will automatically map matching fields like orderId, status, totalAmount, etc.
}