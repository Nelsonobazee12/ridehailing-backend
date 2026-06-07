package com.ridehailing.backend.service;

import com.ridehailing.backend.model.dto.response.FareEstimateResponse;
import com.ridehailing.backend.model.enums.VehicleType;

public interface PricingService {
    FareEstimateResponse estimateFare(Double pickupLat, Double pickupLng,
                                      Double destLat, Double destLng,
                                      VehicleType vehicleType);
    Double calculateActualFare(Double distanceKm, VehicleType vehicleType,
                               Boolean surgeApplied, Double surgeMultiplier);
    double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2);
}