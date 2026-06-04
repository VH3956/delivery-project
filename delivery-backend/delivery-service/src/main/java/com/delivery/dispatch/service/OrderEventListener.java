package com.delivery.dispatch.service;

import com.delivery.dispatch.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final LocationTrackingService locationTrackingService;
    private final AddressResolverService addressResolverService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "order-created-topic", groupId = "delivery-dispatch-group-v2")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("=======================================================");
        log.info("⚡ KAFKA EVENT RECEIVED: NEW ORDER ⚡");
        log.info("Order ID: {}", event.getOrderId());

        // 1. Get the GPS Coordinates for the Pickup Address
        double[] coords = addressResolverService.getCoordinates(event.getPickupAddressId());
        double longitude = coords[0];
        double latitude = coords[1];

        log.info("📍 Pickup Coordinates: [{}, {}]", longitude, latitude);

        // 2. Ask Redis for all drivers within a 5km radius
        double searchRadiusKm = 5.0;
        List<String> nearbyDrivers = locationTrackingService.findNearbyDrivers(longitude, latitude, searchRadiusKm);

        // 3. Dispatch!
        if (nearbyDrivers.isEmpty()) {
            log.warn("⚠️ No drivers found within {}km of the pickup location.", searchRadiusKm);
            // In a real app, you might wait 30 seconds and retry, or expand the radius to 10km!
        } else {
            log.info("✅ Found {} drivers nearby!", nearbyDrivers.size());

            for (String driverId : nearbyDrivers) {
                log.info(" 🚀 PUSHING REAL-TIME WEBSOCKET NOTIFICATION TO: {}", driverId);

                // It sends the Order ID down the exact tunnel the Shipper is listening to.
                String destinationTopic = "/topic/driver/" + driverId;
                messagingTemplate.convertAndSend(destinationTopic, event.getOrderId());
            }
        }
        log.info("=======================================================");
    }
}