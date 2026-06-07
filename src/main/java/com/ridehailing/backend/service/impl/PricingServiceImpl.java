package com.ridehailing.backend.service.impl;

import com.ridehailing.backend.model.dto.response.FareEstimateResponse;
import com.ridehailing.backend.model.enums.VehicleType;
import com.ridehailing.backend.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.GeoResults;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Base fares in Naira per km by vehicle type
    private static final double BIKE_BASE_FARE = 100.0;
    private static final double BIKE_PER_KM = 50.0;

    private static final double TRICYCLE_BASE_FARE = 150.0;
    private static final double TRICYCLE_PER_KM = 70.0;

    private static final double SEDAN_BASE_FARE = 300.0;
    private static final double SEDAN_PER_KM = 120.0;

    private static final double SUV_BASE_FARE = 500.0;
    private static final double SUV_PER_KM = 180.0;

    private static final double VAN_BASE_FARE = 600.0;
    private static final double VAN_PER_KM = 200.0;

    private static final double DRIVER_SHARE = 0.80;
    private static final double SURGE_THRESHOLD = 0.3; // 30% more demand than supply

    @Override
    public FareEstimateResponse estimateFare(Double pickupLat, Double pickupLng,
                                             Double destLat, Double destLng,
                                             VehicleType vehicleType) {
        double distanceKm = calculateDistance(pickupLat, pickupLng, destLat, destLng);
        double baseFare = getBaseFare(vehicleType);
        double perKmRate = getPerKmRate(vehicleType);

        double surgeMultiplier = calculateSurgeMultiplier(pickupLat, pickupLng);
        boolean surgeApplied = surgeMultiplier > 1.0;

        double estimatedFare = (baseFare + (perKmRate * distanceKm)) * surgeMultiplier;
        estimatedFare = Math.round(estimatedFare / 50.0) * 50.0; // Round to nearest 50 Naira

        return FareEstimateResponse.builder()
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .baseFare(baseFare)
                .estimatedFare(estimatedFare)
                .surgeApplied(surgeApplied)
                .surgeMultiplier(surgeMultiplier)
                .currency("NGN")
                .vehicleType(vehicleType.name())
                .build();
    }

    @Override
    public Double calculateActualFare(Double distanceKm, VehicleType vehicleType,
                                      Boolean surgeApplied, Double surgeMultiplier) {
        double baseFare = getBaseFare(vehicleType);
        double perKmRate = getPerKmRate(vehicleType);
        double multiplier = (surgeApplied != null && surgeApplied) ? surgeMultiplier : 1.0;
        double fare = (baseFare + (perKmRate * distanceKm)) * multiplier;
        return Math.round(fare / 50.0) * 50.0;
    }

    // Haversine formula — accurate distance between two GPS coordinates
    @Override
    public double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double calculateSurgeMultiplier(Double lat, Double lng) {
        try {
            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo()
                    .radius("drivers:geo",
                            new org.springframework.data.geo.Circle(
                                    new org.springframework.data.geo.Point(lng, lat),
                                    new org.springframework.data.geo.Distance(3,
                                            org.springframework.data.redis.connection.RedisGeoCommands
                                                    .DistanceUnit.KILOMETERS)));

            long onlineDrivers = results == null ? 0 : results.getContent().size();

            if (onlineDrivers == 0) return 1.5;
            if (onlineDrivers < 3) return 1.3;
            if (onlineDrivers < 6) return 1.1;
            return 1.0;

        } catch (Exception e) {
            log.warn("Could not calculate surge multiplier: {}", e.getMessage());
            return 1.0;
        }
    }

    private double getBaseFare(VehicleType type) {
        return switch (type) {
            case BIKE -> BIKE_BASE_FARE;
            case TRICYCLE -> TRICYCLE_BASE_FARE;
            case SEDAN -> SEDAN_BASE_FARE;
            case SUV -> SUV_BASE_FARE;
            case VAN -> VAN_BASE_FARE;
        };
    }

    private double getPerKmRate(VehicleType type) {
        return switch (type) {
            case BIKE -> BIKE_PER_KM;
            case TRICYCLE -> TRICYCLE_PER_KM;
            case SEDAN -> SEDAN_PER_KM;
            case SUV -> SUV_PER_KM;
            case VAN -> VAN_PER_KM;
        };
    }
}