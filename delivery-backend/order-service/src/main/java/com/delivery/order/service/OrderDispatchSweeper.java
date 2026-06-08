package com.delivery.order.service;

import com.delivery.order.entity.Order;
import com.delivery.order.enums.OrderStatus;
import com.delivery.order.event.OrderCreatedEvent;
import com.delivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDispatchSweeper {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    // This method will automatically run every 30 seconds (30,000 milliseconds)
    @Scheduled(fixedDelay = 30000)
    public void retryStuckOrders() {

        // 1. Find all orders that haven't been picked up by a driver yet
        List<Order> pendingOrders = orderRepository.findAllByStatus(OrderStatus.CREATED);

        if (pendingOrders.isEmpty()) {
            return; // System is healthy, nothing to do!
        }

        log.info("🧹 SWEEPER WOKE UP: Found {} orders stuck in CREATED state. Retrying dispatch...", pendingOrders.size());

        // 2. Loop through them and throw them back into Kafka
        for (Order order : pendingOrders) {
            
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    
                    // Saved Address Flow
                    .pickupAddressId(order.getPickupAddressId())
                    .deliveryAddressId(order.getDeliveryAddressId())
                    
                    // Map Pin Flow (Add these!)
                    .pickupLat(order.getPickupLat())
                    .pickupLng(order.getPickupLng())
                    .deliveryLat(order.getDeliveryLat())
                    .deliveryLng(order.getDeliveryLng())
                    
                    .deliveryFee(order.getDeliveryFee())
                    .build();
                    
            log.info("🔄 Re-queueing Order ID: {}", order.getId());
            orderEventProducer.publishOrderCreatedEvent(event);
        }
    }
}