package com.ridehailing.backend.model.entity;

import com.ridehailing.backend.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    // Pickup
    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false)
    private String pickupAddress;

    // Destination
    @Column(nullable = false)
    private Double destinationLatitude;

    @Column(nullable = false)
    private Double destinationLongitude;

    @Column(nullable = false)
    private String destinationAddress;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    private CancellationReason cancellationReason;

    private String cancellationNote;

    // Vehicle
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    // Pricing
    private Double estimatedFare;
    private Double actualFare;
    private Double distanceKm;
    private Double driverEarnings;
    private Double platformFee;
    private Boolean surgeApplied;
    private Double surgeMultiplier;

    // Payment
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String paystackReference;

    // Ratings
    private Integer riderRating;
    private Integer driverRating;
    private String riderReview;
    private String driverReview;

    // Timestamps
    private LocalDateTime acceptedAt;
    private LocalDateTime driverEnRouteAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}