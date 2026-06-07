package com.ridehailing.backend.model.dto.request;

import com.ridehailing.backend.model.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TripRequest {

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Destination latitude is required")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    private Double destinationLongitude;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
}