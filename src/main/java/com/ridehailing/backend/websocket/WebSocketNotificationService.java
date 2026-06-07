package com.ridehailing.backend.websocket;

import com.ridehailing.backend.websocket.event.DriverLocationEvent;
import com.ridehailing.backend.websocket.event.TripRequestEvent;
import com.ridehailing.backend.websocket.event.TripStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // Push trip status update to a specific rider
    public void notifyRiderTripStatus(Long riderId, TripStatusEvent event) {
        String destination = "/queue/trip-status";
        messagingTemplate.convertAndSendToUser(
                riderId.toString(),
                destination,
                event
        );
        log.info("Trip status {} sent to rider {}", event.getStatus(), riderId);
    }

    // Push trip status update to a specific driver
    public void notifyDriverTripStatus(Long driverId, TripStatusEvent event) {
        String destination = "/queue/trip-status";
        messagingTemplate.convertAndSendToUser(
                driverId.toString(),
                destination,
                event
        );
        log.info("Trip status {} sent to driver {}", event.getStatus(), driverId);
    }

    // Push driver live location to rider during a trip
    public void notifyRiderDriverLocation(Long riderId, DriverLocationEvent event) {
        messagingTemplate.convertAndSendToUser(
                riderId.toString(),
                "/queue/driver-location",
                event
        );
    }

    // Broadcast new trip request to nearby drivers
    public void notifyDriverNewTripRequest(Long driverId, TripRequestEvent event) {
        messagingTemplate.convertAndSendToUser(
                driverId.toString(),
                "/queue/trip-request",
                event
        );
        log.info("New trip request {} sent to driver {}", event.getTripId(), driverId);
    }

    // Broadcast to all subscribers of a trip channel
    public void broadcastTripUpdate(Long tripId, TripStatusEvent event) {
        messagingTemplate.convertAndSend("/topic/trips/" + tripId, event);
    }
}