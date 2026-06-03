package com.delivery.order.service;

import com.delivery.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // The name of the channel we are broadcasting on
    private static final String TOPIC = "order-created-topic";

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("📢 Publishing OrderCreatedEvent to Kafka Topic '{}': {}", TOPIC, event.getOrderId());

        // We use the orderId as the "Key" to ensure messages for the same order stay in order
        kafkaTemplate.send(TOPIC, event.getOrderId(), event);
    }
}