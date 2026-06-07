package com.ridehailing.backend.service;

import com.ridehailing.backend.model.dto.request.CancelTripRequest;
import com.ridehailing.backend.model.dto.request.RatingRequest;
import com.ridehailing.backend.model.dto.request.TripRequest;
import com.ridehailing.backend.model.dto.response.FareEstimateResponse;
import com.ridehailing.backend.model.dto.response.TripResponse;
import com.ridehailing.backend.model.enums.VehicleType;

import java.util.List;

public interface TripService {
    FareEstimateResponse estimateFare(Double pickupLat, Double pickupLng,
                                      Double destLat, Double destLng,
                                      VehicleType vehicleType);
    TripResponse requestTrip(String riderEmail, TripRequest request);
    TripResponse acceptTrip(String driverEmail, Long tripId);
    TripResponse updateTripStatus(String driverEmail, Long tripId, String action);
    TripResponse cancelTrip(String email, Long tripId, CancelTripRequest request);
    TripResponse rateTrip(String email, Long tripId, RatingRequest request);
    TripResponse getTripById(Long tripId, String email);
    List<TripResponse> getMyTrips(String email);
}