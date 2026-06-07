package com.ridehailing.backend.model.dto.response;

import com.ridehailing.backend.model.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NearbyDriverResponse {
    private Long driverId;
    private String firstName;
    private String lastName;
    private String phone;
    private VehicleType vehicleType;
    private String plateNumber;
    private Double rating;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}