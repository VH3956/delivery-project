package com.delivery.dispatch.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.delivery.dispatch.event.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        double longitude;
        double latitude;

        if (event.getPickupLat() != null && event.getPickupLng() != null) {

            // Case 1: Map pin / direct coordinates
            longitude = event.getPickupLng();
            latitude = event.getPickupLat();

            log.info("📍 Using coordinates directly from Order Event");

        } else if (event.getPickupAddressId() != null &&
                !event.getPickupAddressId().isBlank()) {

            // Case 2: Saved address -> resolve via User Service
            log.info("📍 Resolving coordinates using Address ID: {}",
                    event.getPickupAddressId());

            double[] coords = addressResolverService.getCoordinates(
                    event.getPickupAddressId());

            longitude = coords[0];
            latitude = coords[1];

        } else {
            // Case 3: Invalid event (neither coordinates nor address ID)
            throw new IllegalStateException(
                    "Order event contains neither coordinates nor address ID for pickup location");
        }

        log.info("📍 Pickup Coordinates: [{}, {}]", longitude, latitude);

        // 1. If this event contains a dropped driver, permanently blacklist them in Redis for this order!
        if (event.getDroppedShipperId() != null) {
            locationTrackingService.blacklistDriverForOrder(event.getOrderId(), event.getDroppedShipperId());
        }

        // 2. Ask Redis for all drivers within a 5km radius
        double searchRadiusKm = 5.0;
        List<String> nearbyDrivers = locationTrackingService.findNearbyDrivers(longitude, latitude, searchRadiusKm);

        // 3. Filter and Prioritize
        List<String> prioritizedDrivers = nearbyDrivers.stream()
                // 🚀 THE FIX: Check the Redis Blacklist instead of just the event payload!
                .filter(driverId -> !locationTrackingService.isDriverBlacklistedForOrder(event.getOrderId(), driverId))
                .sorted((d1, d2) -> Double.compare(
                        locationTrackingService.getShipperRating(d2), 
                        locationTrackingService.getShipperRating(d1)
                ))
                .limit(3) 
                .collect(Collectors.toList());

        // 3. Dispatch!
        if (prioritizedDrivers.isEmpty()) {
            log.warn("⚠️ No drivers found within {}km of the pickup location.", searchRadiusKm);
            // In a real app, you might wait 30 seconds and retry, or expand the radius to 10km!
        } else {
            log.info("✅ Found {} drivers nearby!", prioritizedDrivers.size());

            for (String driverId : prioritizedDrivers) {
                log.info(" 🚀 PUSHING REAL-TIME WEBSOCKET NOTIFICATION TO: {}", driverId);

                // It sends the Order ID down the exact tunnel the Shipper is listening to.
                String destinationTopic = "/topic/driver/" + driverId;
                messagingTemplate.convertAndSend(destinationTopic, event.getOrderId());
            }
        }
        log.info("=======================================================");
    }
}