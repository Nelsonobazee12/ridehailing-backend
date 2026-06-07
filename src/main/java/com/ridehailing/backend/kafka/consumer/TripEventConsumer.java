package com.ridehailing.backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.backend.kafka.KafkaTopicConfig;
import com.ridehailing.backend.model.dto.response.TripResponse;
import com.ridehailing.backend.model.entity.DriverProfile;
import com.ridehailing.backend.repository.DriverProfileRepository;
import com.ridehailing.backend.service.SmsService;
import com.ridehailing.backend.websocket.WebSocketNotificationService;
import com.ridehailing.backend.websocket.event.TripRequestEvent;
import com.ridehailing.backend.websocket.event.TripStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripEventConsumer {

    private final WebSocketNotificationService notificationService;
    private final SmsService smsService;
    private final DriverProfileRepository driverProfileRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.TRIP_REQUESTED_TOPIC,
            groupId = "ridehailing-group")
    public void onTripRequested(String message) {
        try {
            TripResponse trip = objectMapper.readValue(message, TripResponse.class);
            log.info("Consumed trip.requested: tripId={}", trip.getId());

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

            // SMS notifications
            String plateNumber = trip.getPlateNumber() != null ? trip.getPlateNumber() : "N/A";
            smsService.notifyTripAccepted(
                    trip.getRiderPhone(),
                    trip.getRiderName(),
                    trip.getDriverName(),
                    plateNumber
            );
            if (trip.getDriverPhone() != null) {
                smsService.notifyDriverTripAccepted(
                        trip.getDriverPhone(),
                        trip.getRiderName(),
                        trip.getPickupAddress()
                );
            }

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

            // SMS notifications
            smsService.notifyTripCompleted(
                    trip.getRiderPhone(),
                    trip.getRiderName(),
                    trip.getActualFare(),
                    trip.getDistanceKm()
            );
            if (trip.getDriverPhone() != null && trip.getDriverEarnings() != null) {
                smsService.notifyDriverTripCompleted(
                        trip.getDriverPhone(),
                        trip.getDriverName(),
                        trip.getDriverEarnings()
                );
            }

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

            // SMS notifications
            String reason = trip.getCancellationReason() != null
                    ? trip.getCancellationReason().name() : "Unknown";

            smsService.notifyTripCancelled(
                    trip.getRiderPhone(), trip.getRiderName(), reason);

            if (trip.getDriverPhone() != null && trip.getDriverName() != null) {
                smsService.notifyTripCancelled(
                        trip.getDriverPhone(), trip.getDriverName(), reason);
            }

        } catch (Exception e) {
            log.error("Error processing trip.cancelled: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TRIP_STATUS_UPDATED_TOPIC,
            groupId = "ridehailing-group")
    public void onTripStatusUpdated(String message) {
        try {
            TripResponse trip = objectMapper.readValue(message, TripResponse.class);
            log.info("Consumed trip.status.updated: tripId={} status={}",
                    trip.getId(), trip.getStatus());

            String smsMessage = switch (trip.getStatus()) {
                case DRIVER_EN_ROUTE -> "Your driver is on the way to your pickup location!";
                case ARRIVED -> "Your driver has arrived at your pickup location!";
                case IN_PROGRESS -> "Your trip has started. Destination: "
                        + trip.getDestinationAddress();
                default -> null;
            };

            TripStatusEvent event = TripStatusEvent.builder()
                    .tripId(trip.getId())
                    .status(trip.getStatus())
                    .message(smsMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Push WebSocket update to both rider and driver
            notificationService.notifyRiderTripStatus(trip.getRiderId(), event);
            if (trip.getDriverId() != null) {
                notificationService.notifyDriverTripStatus(trip.getDriverId(), event);
            }
            notificationService.broadcastTripUpdate(trip.getId(), event);

            // Send SMS to rider
            if (smsMessage != null && trip.getRiderPhone() != null) {
                smsService.sendSms(trip.getRiderPhone(), smsMessage);
            }

        } catch (Exception e) {
            log.error("Error processing trip.status.updated: {}", e.getMessage());
        }
    }
}