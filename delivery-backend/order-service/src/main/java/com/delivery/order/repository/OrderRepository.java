package com.delivery.order.repository;

import com.delivery.order.entity.Order;
import com.delivery.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // For Customers: View their own order history
    List<Order> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    // For Shippers: View orders they are currently handling
    List<Order> findAllByShipperIdOrderByCreatedAtDesc(String shipperId);

    // For System/Admin: Find all orders currently looking for a driver
    List<Order> findAllByStatus(OrderStatus status);

    // ==========================================
    // ADM-04: Pagination Queries for Admin
    // ==========================================
    Page<Order> findAll(Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // ==========================================
    // ADM-06: Dashboard Statistics Queries
    // ==========================================
    
    // 1. Get total revenue (only count COMPLETED orders)
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    // 2. Get count of orders grouped by their status
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();
}