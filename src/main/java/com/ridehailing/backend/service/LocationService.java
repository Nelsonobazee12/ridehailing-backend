package com.ridehailing.backend.service;

import com.ridehailing.backend.model.dto.request.LocationUpdateRequest;
import com.ridehailing.backend.model.dto.response.NearbyDriverResponse;
import com.ridehailing.backend.model.enums.VehicleType;

import java.util.List;

public interface LocationService {
    void updateDriverLocation(Long driverId, LocationUpdateRequest request);
    List<NearbyDriverResponse> findNearbyDrivers(Double latitude, Double longitude,
                                                 Double radiusKm, VehicleType vehicleType);
    void removeDriverLocation(Long driverId);
}