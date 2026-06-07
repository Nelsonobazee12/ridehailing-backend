package com.ridehailing.backend.websocket.event;

import com.ridehailing.backend.model.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStatusEvent {
    private Long tripId;
    private TripStatus status;
    private String message;
    private LocalDateTime timestamp;
}