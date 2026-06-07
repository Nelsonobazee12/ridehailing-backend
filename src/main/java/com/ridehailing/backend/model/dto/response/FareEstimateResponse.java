package com.ridehailing.backend.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FareEstimateResponse {
    private Double distanceKm;
    private Double baseFare;
    private Double estimatedFare;
    private Boolean surgeApplied;
    private Double surgeMultiplier;
    private String currency;
    private String vehicleType;
}