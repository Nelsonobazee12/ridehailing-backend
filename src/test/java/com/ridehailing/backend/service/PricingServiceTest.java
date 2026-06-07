package com.ridehailing.backend.service;

import com.ridehailing.backend.model.dto.response.FareEstimateResponse;
import com.ridehailing.backend.model.enums.VehicleType;
import com.ridehailing.backend.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private GeoOperations<String, Object> geoOperations;

    private PricingServiceImpl pricingService;

    // Lagos coordinates
    private static final double PICKUP_LAT = 6.5244;
    private static final double PICKUP_LNG = 3.3792;
    private static final double DEST_LAT = 6.6018;
    private static final double DEST_LNG = 3.3515;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl(redisTemplate);
    }

    private void mockRedisGeoWithDriverCount(int driverCount) {
        List<GeoResult<RedisGeoCommands.GeoLocation<Object>>> resultList =
                java.util.Collections.nCopies(driverCount,
                        new GeoResult<>(
                                new RedisGeoCommands.GeoLocation<>("1", null),
                                new org.springframework.data.geo.Distance(0)));

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(geoOperations.radius(eq("drivers:geo"), any()))
                .thenReturn(new GeoResults<>(resultList));
    }

    @Test
    @DisplayName("Should calculate distance using Haversine formula")
    void shouldCalculateDistanceCorrectly() {
        double distance = pricingService.calculateDistance(
                PICKUP_LAT, PICKUP_LNG, DEST_LAT, DEST_LNG);

        // Lagos to Ikeja is roughly 9-11 km
        assertThat(distance).isBetween(8.0, 12.0);
    }

    @Test
    @DisplayName("Should estimate fare for BIKE vehicle type")
    void shouldEstimateFareForBike() {
        mockRedisGeoWithDriverCount(0);

        FareEstimateResponse response = pricingService.estimateFare(
                PICKUP_LAT, PICKUP_LNG, DEST_LAT, DEST_LNG, VehicleType.BIKE);

        assertThat(response).isNotNull();
        assertThat(response.getEstimatedFare()).isGreaterThan(0);
        assertThat(response.getDistanceKm()).isGreaterThan(0);
        assertThat(response.getCurrency()).isEqualTo("NGN");
        assertThat(response.getVehicleType()).isEqualTo("BIKE");
    }

    @Test
    @DisplayName("Should estimate higher fare for SUV than BIKE")
    void shouldEstimateHigherFareForSuvThanBike() {
        mockRedisGeoWithDriverCount(10);

        FareEstimateResponse bikeFare = pricingService.estimateFare(
                PICKUP_LAT, PICKUP_LNG, DEST_LAT, DEST_LNG, VehicleType.BIKE);

        FareEstimateResponse suvFare = pricingService.estimateFare(
                PICKUP_LAT, PICKUP_LNG, DEST_LAT, DEST_LNG, VehicleType.SUV);

        assertThat(suvFare.getEstimatedFare()).isGreaterThan(bikeFare.getEstimatedFare());
    }

    @Test
    @DisplayName("Should apply surge when no drivers available")
    void shouldApplySurgeWhenNoDriversAvailable() {
        mockRedisGeoWithDriverCount(0);

        FareEstimateResponse response = pricingService.estimateFare(
                PICKUP_LAT, PICKUP_LNG, DEST_LAT, DEST_LNG, VehicleType.SEDAN);

        assertThat(response.getSurgeApplied()).isTrue();
        assertThat(response.getSurgeMultiplier()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("Should round fare to nearest 50 Naira")
    void shouldRoundFareToNearest50() {
        mockRedisGeoWithDriverCount(0);

        FareEstimateResponse response = pricingService.estimateFare(
                PICKUP_LAT, PICKUP_LNG, DEST_LAT, DEST_LNG, VehicleType.SEDAN);

        assertThat(response.getEstimatedFare() % 50).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate actual fare correctly")
    void shouldCalculateActualFare() {
        Double fare = pricingService.calculateActualFare(
                10.0, VehicleType.BIKE, false, 1.0);

        // BIKE: 100 base + (50 * 10km) = 600 Naira
        assertThat(fare).isEqualTo(600.0);
    }

    @Test
    @DisplayName("Should apply surge multiplier to actual fare")
    void shouldApplySurgeToActualFare() {
        Double normalFare = pricingService.calculateActualFare(
                10.0, VehicleType.BIKE, false, 1.0);

        Double surgeFare = pricingService.calculateActualFare(
                10.0, VehicleType.BIKE, true, 1.5);

        assertThat(surgeFare).isGreaterThan(normalFare);
    }
}