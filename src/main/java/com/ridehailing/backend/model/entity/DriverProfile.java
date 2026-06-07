package com.ridehailing.backend.model.entity;

import com.ridehailing.backend.model.enums.DriverStatus;
import com.ridehailing.backend.model.enums.VehicleType;
import com.ridehailing.backend.model.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Vehicle info
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String vehicleMake;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(nullable = false)
    private String vehicleYear;

    @Column(nullable = false, unique = true)
    private String plateNumber;

    private String vehicleColor;

    // Documents
    private String licenseNumber;
    private String licensePhotoUrl;
    private String vehiclePhotoUrl;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus driverStatus = DriverStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    // Ratings
    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer totalTrips = 0;

    @Builder.Default
    private Integer totalRatings = 0;

    // Earnings
    @Builder.Default
    private Double totalEarnings = 0.0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}