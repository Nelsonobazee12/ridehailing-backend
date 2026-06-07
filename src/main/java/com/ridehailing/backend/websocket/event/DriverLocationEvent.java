package com.ridehailing.backend.websocket.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationEvent {
    private Long driverId;
    private Long riderId;
    private Long tripId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}