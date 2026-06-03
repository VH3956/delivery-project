package com.delivery.order.repository;

import com.delivery.order.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, String> {

    // Fetch the tracking history for a specific order
    List<OrderTimeline> findAllByOrderIdOrderByCreatedAtAsc(String orderId);
}