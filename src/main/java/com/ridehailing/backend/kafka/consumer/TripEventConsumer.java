package com.ridehailing.backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.backend.kafka.KafkaTopicConfig;
import com.ridehailing.backend.model.dto.response.TripResponse;
import com.ridehailing.backend.websocket.WebSocketNotificationService;
import com.ridehailing.backend.websocket.event.TripRequestEvent;
import com.ridehailing.backend.websocket.event.TripStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripEventConsumer {

    private final WebSocketNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.TRIP_REQUESTED_TOPIC,
            groupId = "ridehailing-group")
    public void onTripRequested(String message) {
        try {
            TripResponse trip = objectMapper.readValue(message, TripResponse.class);
            log.info("Consumed trip.requested: tripId={}", trip.getId());

            // Notify driver of new trip request
            if (trip.getDriverId() != null) {
                TripRequestEvent event = TripRequestEvent.builder()
                        .tripId(trip.getId())
                        .riderId(trip.getRiderId())
                        .riderName(trip.getRiderName())
                        .riderPhone(trip.getRiderPhone())
                        .pickupLatitude(trip.getPickupLatitude())
                        .pickupLongitude(trip.getPickupLongitude())
                        .pickupAddress(trip.getPickupAddress())
                        .destinationAddress(trip.getDestinationAddress())
                        .estimatedFare(trip.getEstimatedFare())
                        .distanceKm(trip.getDistanceKm())
                        .vehicleType(trip.getVehicleType())
                        .timestamp(LocalDateTime.now())
                        .build();

                notificationService.notifyDriverNewTripRequest(trip.getDriverId(), event);
            }
        } catch (Exception e) {
            log.error("Error processing trip.requested: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TRIP_ACCEPTED_TOPIC,
            groupId = "ridehailing-group")
    public void onTripAccepted(String message) {
        try {
            TripResponse trip = objectMapper.readValue(message, TripResponse.class);
            log.info("Consumed trip.accepted: tripId={}", trip.getId());

            TripStatusEvent event = TripStatusEvent.builder()
                    .tripId(trip.getId())
                    .status(trip.getStatus())
                    .message("Your driver is on the way!")
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.notifyRiderTripStatus(trip.getRiderId(), event);
            notificationService.broadcastTripUpdate(trip.getId(), event);

        } catch (Exception e) {
            log.error("Error processing trip.accepted: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TRIP_COMPLETED_TOPIC,
            groupId = "ridehailing-group")
    public void onTripCompleted(String message) {
        try {
            TripResponse trip = objectMapper.readValue(message, TripResponse.class);
            log.info("Consumed trip.completed: tripId={}", trip.getId());

            TripStatusEvent event = TripStatusEvent.builder()
                    .tripId(trip.getId())
                    .status(trip.getStatus())
                    .message("Trip completed. Fare: NGN " + trip.getActualFare())
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.notifyRiderTripStatus(trip.getRiderId(), event);
            if (trip.getDriverId() != null) {
                notificationService.notifyDriverTripStatus(trip.getDriverId(), event);
            }
            notificationService.broadcastTripUpdate(trip.getId(), event);

        } catch (Exception e) {
            log.error("Error processing trip.completed: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TRIP_CANCELLED_TOPIC,
            groupId = "ridehailing-group")
    public void onTripCancelled(String message) {
        try {
            TripResponse trip = objectMapper.readValue(message, TripResponse.class);
            log.info("Consumed trip.cancelled: tripId={}", trip.getId());

            TripStatusEvent event = TripStatusEvent.builder()
                    .tripId(trip.getId())
                    .status(trip.getStatus())
                    .message("Trip cancelled: " + trip.getCancellationReason())
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationService.notifyRiderTripStatus(trip.getRiderId(), event);
            if (trip.getDriverId() != null) {
                notificationService.notifyDriverTripStatus(trip.getDriverId(), event);
            }

        } catch (Exception e) {
            log.error("Error processing trip.cancelled: {}", e.getMessage());
        }
    }
}