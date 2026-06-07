package com.ridehailing.backend.controller;

import com.ridehailing.backend.model.dto.request.CancelTripRequest;
import com.ridehailing.backend.model.dto.request.RatingRequest;
import com.ridehailing.backend.model.dto.request.TripRequest;
import com.ridehailing.backend.model.dto.response.ApiResponse;
import com.ridehailing.backend.model.dto.response.FareEstimateResponse;
import com.ridehailing.backend.model.dto.response.TripResponse;
import com.ridehailing.backend.model.enums.VehicleType;
import com.ridehailing.backend.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping("/estimate")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<ApiResponse<FareEstimateResponse>> estimateFare(
            @RequestParam Double pickupLat,
            @RequestParam Double pickupLng,
            @RequestParam Double destLat,
            @RequestParam Double destLng,
            @RequestParam VehicleType vehicleType) {
        FareEstimateResponse response = tripService.estimateFare(
                pickupLat, pickupLng, destLat, destLng, vehicleType);
        return ResponseEntity.ok(ApiResponse.success("Fare estimated", response));
    }

    @PostMapping("/request")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<ApiResponse<TripResponse>> requestTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.requestTrip(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip requested successfully", response));
    }

    @PostMapping("/{tripId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<TripResponse>> acceptTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId) {
        TripResponse response = tripService.acceptTrip(userDetails.getUsername(), tripId);
        return ResponseEntity.ok(ApiResponse.success("Trip accepted", response));
    }

    @PatchMapping("/{tripId}/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<TripResponse>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId,
            @RequestParam String action) {
        TripResponse response = tripService.updateTripStatus(
                userDetails.getUsername(), tripId, action);
        return ResponseEntity.ok(ApiResponse.success("Trip status updated", response));
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId,
            @Valid @RequestBody CancelTripRequest request) {
        TripResponse response = tripService.cancelTrip(
                userDetails.getUsername(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success("Trip cancelled", response));
    }

    @PostMapping("/{tripId}/rate")
    public ResponseEntity<ApiResponse<TripResponse>> rateTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId,
            @Valid @RequestBody RatingRequest request) {
        TripResponse response = tripService.rateTrip(
                userDetails.getUsername(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success("Rating submitted", response));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId) {
        TripResponse response = tripService.getTripById(tripId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Trip retrieved", response));
    }

    @GetMapping("/my-trips")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TripResponse> trips = tripService.getMyTrips(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(trips.size() + " trip(s) found", trips));
    }
}