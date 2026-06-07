package com.ridehailing.backend.service;

import com.ridehailing.backend.model.dto.request.LoginRequest;
import com.ridehailing.backend.model.dto.request.RefreshTokenRequest;
import com.ridehailing.backend.model.dto.request.RegisterRequest;
import com.ridehailing.backend.model.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String email);
}