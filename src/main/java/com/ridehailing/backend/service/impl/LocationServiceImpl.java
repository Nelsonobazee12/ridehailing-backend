package com.ridehailing.backend.service.impl;

import com.ridehailing.backend.exception.AppException;
import com.ridehailing.backend.model.dto.request.LocationUpdateRequest;
import com.ridehailing.backend.model.dto.response.NearbyDriverResponse;
import com.ridehailing.backend.model.entity.DriverProfile;
import com.ridehailing.backend.model.enums.DriverStatus;
import com.ridehailing.backend.model.enums.VehicleType;
import com.ridehailing.backend.model.redis.DriverLocation;
import com.ridehailing.backend.repository.DriverLocationRepository;
import com.ridehailing.backend.repository.DriverProfileRepository;
import com.ridehailing.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final String GEO_KEY = "drivers:geo";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DriverLocationRepository driverLocationRepository;
    private final DriverProfileRepository driverProfileRepository;

    @Override
    public void updateDriverLocation(Long driverId, LocationUpdateRequest request) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new AppException("Driver profile not found", HttpStatus.NOT_FOUND));

        // Update Redis GEO index for proximity search
        redisTemplate.opsForGeo().add(
                GEO_KEY,
                new Point(request.getLongitude(), request.getLatitude()),
                driverId.toString()
        );

        // Update driver location hash in Redis
        DriverLocation location = DriverLocation.builder()
                .driverId(driverId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(profile.getDriverStatus())
                .vehicleType(profile.getVehicleType())
                .firstName(profile.getUser().getFirstName())
                .lastName(profile.getUser().getLastName())
                .phone(profile.getUser().getPhone())
                .rating(profile.getRating())
                .plateNumber(profile.getPlateNumber())
                .ttl(300L)
                .build();

        driverLocationRepository.save(location);
        log.info("Driver {} location updated: {}, {}", driverId,
                request.getLatitude(), request.getLongitude());
    }

    @Override
    public List<NearbyDriverResponse> findNearbyDrivers(Double latitude, Double longitude,
                                                        Double radiusKm, VehicleType vehicleType) {
        Circle circle = new Circle(
                new Point(longitude, latitude),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(20);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(GEO_KEY, circle, args);

        if (results == null) return List.of();

        List<NearbyDriverResponse> nearby = new ArrayList<>();

        for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
            String driverIdStr = result.getContent().getName().toString();
            Long driverId = Long.parseLong(driverIdStr);

            driverLocationRepository.findById(driverId).ifPresent(location -> {
                // Filter by status and vehicle type
                if (location.getStatus() != DriverStatus.ONLINE) return;
                if (vehicleType != null && location.getVehicleType() != vehicleType) return;

                nearby.add(NearbyDriverResponse.builder()
                        .driverId(driverId)
                        .firstName(location.getFirstName())
                        .lastName(location.getLastName())
                        .phone(location.getPhone())
                        .vehicleType(location.getVehicleType())
                        .plateNumber(location.getPlateNumber())
                        .rating(location.getRating())
                        .latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .distanceKm(result.getDistance().getValue())
                        .build());
            });
        }

        return nearby;
    }

    @Override
    public void removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(GEO_KEY, driverId.toString());
        driverLocationRepository.deleteById(driverId);
        log.info("Driver {} removed from location index", driverId);
    }
}