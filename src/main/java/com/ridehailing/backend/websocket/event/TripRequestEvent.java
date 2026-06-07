package com.ridehailing.backend.websocket.event;

import com.ridehailing.backend.model.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRequestEvent {
    private Long tripId;
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private String destinationAddress;
    private Double estimatedFare;
    private Double distanceKm;
    private VehicleType vehicleType;
    private LocalDateTime timestamp;
}