package com.ridehailing.backend.model.dto.response;

import com.ridehailing.backend.model.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TripResponse {
    private Long id;

    // Rider
    private Long riderId;
    private String riderName;
    private String riderPhone;

    // Driver
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String plateNumber;
    private Double driverEarnings;

    // Locations
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationAddress;

    // Status
    private TripStatus status;
    private CancellationReason cancellationReason;
    private String cancellationNote;

    // Vehicle
    private VehicleType vehicleType;

    // Pricing
    private Double estimatedFare;
    private Double actualFare;
    private Double distanceKm;
    private Boolean surgeApplied;
    private Double surgeMultiplier;

    // Payment
    private PaymentStatus paymentStatus;

    // Ratings
    private Integer riderRating;
    private Integer driverRating;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}