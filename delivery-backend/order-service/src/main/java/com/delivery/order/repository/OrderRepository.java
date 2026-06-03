package com.delivery.order.repository;

import com.delivery.order.entity.Order;
import com.delivery.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // For Customers: View their own order history
    List<Order> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    // For Shippers: View orders they are currently handling
    List<Order> findAllByShipperIdOrderByCreatedAtDesc(String shipperId);

    // For System/Admin: Find all orders currently looking for a driver
    List<Order> findAllByStatus(OrderStatus status);
}