package com.ridehailing.backend.service.impl;

import com.ridehailing.backend.exception.AppException;
import com.ridehailing.backend.kafka.producer.TripEventProducer;
import com.ridehailing.backend.model.dto.request.CancelTripRequest;
import com.ridehailing.backend.model.dto.request.RatingRequest;
import com.ridehailing.backend.model.dto.request.TripRequest;
import com.ridehailing.backend.model.dto.response.FareEstimateResponse;
import com.ridehailing.backend.model.dto.response.TripResponse;
import com.ridehailing.backend.model.entity.DriverProfile;
import com.ridehailing.backend.model.entity.Trip;
import com.ridehailing.backend.model.entity.User;
import com.ridehailing.backend.model.enums.*;
import com.ridehailing.backend.repository.DriverProfileRepository;
import com.ridehailing.backend.repository.TripRepository;
import com.ridehailing.backend.repository.UserRepository;
import com.ridehailing.backend.service.PricingService;
import com.ridehailing.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PricingService pricingService;
    private final TripEventProducer tripEventProducer;

    @Override
    public FareEstimateResponse estimateFare(Double pickupLat, Double pickupLng,
                                             Double destLat, Double destLng,
                                             VehicleType vehicleType) {
        return pricingService.estimateFare(pickupLat, pickupLng, destLat, destLng, vehicleType);
    }

    @Override
    @Transactional
    public TripResponse requestTrip(String riderEmail, TripRequest request) {
        User rider = getUser(riderEmail);

        // Check rider has no active trip
        tripRepository.findActiveRiderTrip(rider.getId()).ifPresent(t -> {
            throw new AppException("You already have an active trip", HttpStatus.CONFLICT);
        });

        FareEstimateResponse fare = pricingService.estimateFare(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDestinationLatitude(), request.getDestinationLongitude(),
                request.getVehicleType()
        );

        Trip trip = Trip.builder()
                .rider(rider)
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .pickupAddress(request.getPickupAddress())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .destinationAddress(request.getDestinationAddress())
                .vehicleType(request.getVehicleType())
                .estimatedFare(fare.getEstimatedFare())
                .distanceKm(fare.getDistanceKm())
                .surgeApplied(fare.getSurgeApplied())
                .surgeMultiplier(fare.getSurgeMultiplier())
                .status(TripStatus.REQUESTED)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        tripRepository.save(trip);
        tripEventProducer.publishTripRequested(toResponse(trip));
        log.info("Trip {} requested by rider {}", trip.getId(), rider.getEmail());
        return toResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse acceptTrip(String driverEmail, Long tripId) {
        User driver = getUser(driverEmail);
        DriverProfile profile = driverProfileRepository.findByUserId(driver.getId())
                .orElseThrow(() -> new AppException("Driver profile not found", HttpStatus.NOT_FOUND));

        if (profile.getDriverStatus() != DriverStatus.ONLINE) {
            throw new AppException("Driver must be online to accept trips", HttpStatus.BAD_REQUEST);
        }

        if (tripRepository.countActiveTripsForDriver(driver.getId()) > 0) {
            throw new AppException("You already have an active trip", HttpStatus.CONFLICT);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException("Trip not found", HttpStatus.NOT_FOUND));

        if (trip.getStatus() != TripStatus.REQUESTED) {
            throw new AppException("Trip is no longer available", HttpStatus.CONFLICT);
        }

        trip.setDriver(driver);
        trip.setStatus(TripStatus.ACCEPTED);
        trip.setAcceptedAt(LocalDateTime.now());

        // Update driver status
        profile.setDriverStatus(DriverStatus.ON_TRIP);
        driverProfileRepository.save(profile);

        tripRepository.save(trip);
        tripEventProducer.publishTripAccepted(toResponse(trip));
        log.info("Trip {} accepted by driver {}", tripId, driverEmail);
        return toResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse updateTripStatus(String driverEmail, Long tripId, String action) {
        User driver = getUser(driverEmail);
        Trip trip = tripRepository.findByIdAndDriverId(tripId, driver.getId())
                .orElseThrow(() -> new AppException("Trip not found or not assigned to you",
                        HttpStatus.NOT_FOUND));

        switch (action.toUpperCase()) {
            case "EN_ROUTE" -> {
                validateTransition(trip.getStatus(), TripStatus.ACCEPTED, TripStatus.DRIVER_EN_ROUTE);
                trip.setStatus(TripStatus.DRIVER_EN_ROUTE);
                trip.setDriverEnRouteAt(LocalDateTime.now());
            }
            case "ARRIVED" -> {
                validateTransition(trip.getStatus(), TripStatus.DRIVER_EN_ROUTE, TripStatus.ARRIVED);
                trip.setStatus(TripStatus.ARRIVED);
                trip.setArrivedAt(LocalDateTime.now());
            }
            case "START" -> {
                validateTransition(trip.getStatus(), TripStatus.ARRIVED, TripStatus.IN_PROGRESS);
                trip.setStatus(TripStatus.IN_PROGRESS);
                trip.setStartedAt(LocalDateTime.now());
            }
            case "COMPLETE" -> {
                validateTransition(trip.getStatus(), TripStatus.IN_PROGRESS, TripStatus.COMPLETED);
                completeTrip(trip, driver);
            }
            default -> throw new AppException("Invalid action: " + action, HttpStatus.BAD_REQUEST);
        }

        tripRepository.save(trip);
        log.info("Trip {} status updated to {} by driver {}", tripId, trip.getStatus(), driverEmail);
        return toResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse cancelTrip(String email, Long tripId, CancelTripRequest request) {
        User user = getUser(email);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException("Trip not found", HttpStatus.NOT_FOUND));

        // Validate ownership
        boolean isRider = trip.getRider().getId().equals(user.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getId().equals(user.getId());

        if (!isRider && !isDriver) {
            throw new AppException("You are not part of this trip", HttpStatus.FORBIDDEN);
        }

        // Can only cancel before IN_PROGRESS
        if (trip.getStatus() == TripStatus.IN_PROGRESS ||
                trip.getStatus() == TripStatus.COMPLETED ||
                trip.getStatus() == TripStatus.CANCELLED) {
            throw new AppException("Trip cannot be cancelled at this stage", HttpStatus.BAD_REQUEST);
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancellationReason(request.getReason());
        trip.setCancellationNote(request.getNote());
        trip.setCancelledAt(LocalDateTime.now());

        // Free up driver if one was assigned
        if (trip.getDriver() != null) {
            driverProfileRepository.findByUserId(trip.getDriver().getId())
                    .ifPresent(profile -> {
                        profile.setDriverStatus(DriverStatus.ONLINE);
                        driverProfileRepository.save(profile);
                    });
        }

        tripRepository.save(trip);
        tripEventProducer.publishTripCancelled(toResponse(trip));
        log.info("Trip {} cancelled by {}", tripId, email);
        return toResponse(trip);
    }

    @Override
    @Transactional
    public TripResponse rateTrip(String email, Long tripId, RatingRequest request) {
        User user = getUser(email);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException("Trip not found", HttpStatus.NOT_FOUND));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new AppException("Can only rate completed trips", HttpStatus.BAD_REQUEST);
        }

        boolean isRider = trip.getRider().getId().equals(user.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getId().equals(user.getId());

        if (isRider) {
            if (trip.getDriverRating() != null) {
                throw new AppException("You have already rated this trip", HttpStatus.CONFLICT);
            }
            trip.setDriverRating(request.getRating());
            trip.setDriverReview(request.getReview());
            updateDriverRating(trip.getDriver().getId(), request.getRating());
        } else if (isDriver) {
            if (trip.getRiderRating() != null) {
                throw new AppException("You have already rated this trip", HttpStatus.CONFLICT);
            }
            trip.setRiderRating(request.getRating());
            trip.setRiderReview(request.getReview());
        } else {
            throw new AppException("You are not part of this trip", HttpStatus.FORBIDDEN);
        }

        tripRepository.save(trip);
        return toResponse(trip);
    }

    @Override
    public TripResponse getTripById(Long tripId, String email) {
        User user = getUser(email);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException("Trip not found", HttpStatus.NOT_FOUND));

        boolean isRider = trip.getRider().getId().equals(user.getId());
        boolean isDriver = trip.getDriver() != null && trip.getDriver().getId().equals(user.getId());

        if (!isRider && !isDriver) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        return toResponse(trip);
    }

    @Override
    public List<TripResponse> getMyTrips(String email) {
        User user = getUser(email);
        List<Trip> trips = user.getRole() == Role.RIDER
                ? tripRepository.findByRiderIdOrderByCreatedAtDesc(user.getId())
                : tripRepository.findByDriverIdOrderByCreatedAtDesc(user.getId());
        return trips.stream().map(this::toResponse).toList();
    }

    // --- Private helpers ---

    private void completeTrip(Trip trip, User driver) {
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(LocalDateTime.now());

        Double actualFare = pricingService.calculateActualFare(
                trip.getDistanceKm(), trip.getVehicleType(),
                trip.getSurgeApplied(), trip.getSurgeMultiplier());

        trip.setActualFare(actualFare);
        trip.setDriverEarnings(Math.round(actualFare * 0.80 * 100.0) / 100.0);
        trip.setPlatformFee(Math.round(actualFare * 0.20 * 100.0) / 100.0);
        trip.setPaymentStatus(PaymentStatus.PAID);

        // Update driver stats
        driverProfileRepository.findByUserId(driver.getId()).ifPresent(profile -> {
            profile.setDriverStatus(DriverStatus.ONLINE);
            profile.setTotalTrips(profile.getTotalTrips() + 1);
            profile.setTotalEarnings(profile.getTotalEarnings() + trip.getDriverEarnings());
            driverProfileRepository.save(profile);
            tripEventProducer.publishTripCompleted(toResponse(trip));
        });
    }

    private void validateTransition(TripStatus current, TripStatus expected, TripStatus next) {
        if (current != expected) {
            throw new AppException(
                    String.format("Cannot transition to %s from %s", next, current),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void updateDriverRating(Long driverUserId, Integer newRating) {
        driverProfileRepository.findByUserId(driverUserId).ifPresent(profile -> {
            int total = profile.getTotalRatings() + 1;
            double avg = ((profile.getRating() * profile.getTotalRatings()) + newRating) / total;
            profile.setRating(Math.round(avg * 10.0) / 10.0);
            profile.setTotalRatings(total);
            driverProfileRepository.save(profile);
        });
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    private TripResponse toResponse(Trip trip) {
        DriverProfile driverProfile = trip.getDriver() != null
                ? driverProfileRepository.findByUserId(trip.getDriver().getId()).orElse(null)
                : null;

        return TripResponse.builder()
                .id(trip.getId())
                .riderId(trip.getRider().getId())
                .riderName(trip.getRider().getFirstName() + " " + trip.getRider().getLastName())
                .riderPhone(trip.getRider().getPhone())
                .driverId(trip.getDriver() != null ? trip.getDriver().getId() : null)
                .driverName(trip.getDriver() != null
                        ? trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName()
                        : null)
                .driverPhone(trip.getDriver() != null ? trip.getDriver().getPhone() : null)
                .plateNumber(driverProfile != null ? driverProfile.getPlateNumber() : null)
                .pickupLatitude(trip.getPickupLatitude())
                .pickupLongitude(trip.getPickupLongitude())
                .pickupAddress(trip.getPickupAddress())
                .destinationLatitude(trip.getDestinationLatitude())
                .destinationLongitude(trip.getDestinationLongitude())
                .destinationAddress(trip.getDestinationAddress())
                .status(trip.getStatus())
                .cancellationReason(trip.getCancellationReason())
                .cancellationNote(trip.getCancellationNote())
                .vehicleType(trip.getVehicleType())
                .estimatedFare(trip.getEstimatedFare())
                .actualFare(trip.getActualFare())
                .distanceKm(trip.getDistanceKm())
                .surgeApplied(trip.getSurgeApplied())
                .surgeMultiplier(trip.getSurgeMultiplier())
                .paymentStatus(trip.getPaymentStatus())
                .riderRating(trip.getRiderRating())
                .driverRating(trip.getDriverRating())
                .createdAt(trip.getCreatedAt())
                .acceptedAt(trip.getAcceptedAt())
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .build();
    }
}