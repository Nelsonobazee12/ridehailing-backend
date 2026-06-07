package com.ridehailing.backend.service.impl;

import com.ridehailing.backend.exception.AppException;
import com.ridehailing.backend.model.dto.request.DriverProfileRequest;
import com.ridehailing.backend.model.dto.request.LocationUpdateRequest;
import com.ridehailing.backend.model.dto.response.DriverProfileResponse;
import com.ridehailing.backend.model.entity.DriverProfile;
import com.ridehailing.backend.model.entity.User;
import com.ridehailing.backend.model.enums.DriverStatus;
import com.ridehailing.backend.model.enums.Role;
import com.ridehailing.backend.repository.DriverProfileRepository;
import com.ridehailing.backend.repository.UserRepository;
import com.ridehailing.backend.service.DriverProfileService;
import com.ridehailing.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverProfileServiceImpl implements DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public DriverProfileResponse createProfile(String email, DriverProfileRequest request) {
        User user = getUser(email);

        if (user.getRole() != Role.DRIVER) {
            throw new AppException("Only drivers can create a driver profile", HttpStatus.FORBIDDEN);
        }
        if (driverProfileRepository.existsByUserId(user.getId())) {
            throw new AppException("Driver profile already exists", HttpStatus.CONFLICT);
        }
        if (driverProfileRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new AppException("Plate number already registered", HttpStatus.CONFLICT);
        }

        DriverProfile profile = DriverProfile.builder()
                .user(user)
                .vehicleType(request.getVehicleType())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .vehicleYear(request.getVehicleYear())
                .plateNumber(request.getPlateNumber())
                .vehicleColor(request.getVehicleColor())
                .licenseNumber(request.getLicenseNumber())
                .build();

        return toResponse(driverProfileRepository.save(profile));
    }

    @Override
    public DriverProfileResponse getProfile(String email) {
        User user = getUser(email);
        DriverProfile profile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException("Driver profile not found", HttpStatus.NOT_FOUND));
        return toResponse(profile);
    }

    @Override
    @Transactional
    public DriverProfileResponse updateStatus(String email, DriverStatus status) {
        User user = getUser(email);
        DriverProfile profile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException("Driver profile not found", HttpStatus.NOT_FOUND));

        profile.setDriverStatus(status);
        driverProfileRepository.save(profile);

        // Remove from geo index when going offline
        if (status == DriverStatus.OFFLINE) {
            locationService.removeDriverLocation(profile.getId());
        }

        return toResponse(profile);
    }

    @Override
    public void updateLocation(String email, LocationUpdateRequest request) {
        User user = getUser(email);
        DriverProfile profile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException("Driver profile not found", HttpStatus.NOT_FOUND));

        if (profile.getDriverStatus() == DriverStatus.OFFLINE) {
            throw new AppException("Driver must be online to update location", HttpStatus.BAD_REQUEST);
        }

        locationService.updateDriverLocation(profile.getId(), request);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }

    private DriverProfileResponse toResponse(DriverProfile profile) {
        return DriverProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .firstName(profile.getUser().getFirstName())
                .lastName(profile.getUser().getLastName())
                .email(profile.getUser().getEmail())
                .phone(profile.getUser().getPhone())
                .vehicleType(profile.getVehicleType())
                .vehicleMake(profile.getVehicleMake())
                .vehicleModel(profile.getVehicleModel())
                .vehicleYear(profile.getVehicleYear())
                .plateNumber(profile.getPlateNumber())
                .vehicleColor(profile.getVehicleColor())
                .licenseNumber(profile.getLicenseNumber())
                .driverStatus(profile.getDriverStatus())
                .verificationStatus(profile.getVerificationStatus())
                .rating(profile.getRating())
                .totalTrips(profile.getTotalTrips())
                .totalEarnings(profile.getTotalEarnings())
                .build();
    }
}