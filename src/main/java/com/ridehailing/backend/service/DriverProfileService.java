package com.ridehailing.backend.service;

import com.ridehailing.backend.model.dto.request.DriverProfileRequest;
import com.ridehailing.backend.model.dto.request.LocationUpdateRequest;
import com.ridehailing.backend.model.dto.response.DriverProfileResponse;
import com.ridehailing.backend.model.enums.DriverStatus;

public interface DriverProfileService {
    DriverProfileResponse createProfile(String email, DriverProfileRequest request);
    DriverProfileResponse getProfile(String email);
    DriverProfileResponse updateStatus(String email, DriverStatus status);
    void updateLocation(String email, LocationUpdateRequest request);
}