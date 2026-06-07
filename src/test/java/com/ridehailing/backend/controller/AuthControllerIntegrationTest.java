package com.ridehailing.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.backend.BaseIntegrationTest;
import com.ridehailing.backend.model.dto.request.LoginRequest;
import com.ridehailing.backend.model.dto.request.RegisterRequest;
import com.ridehailing.backend.model.enums.Role;
import com.ridehailing.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register rider successfully")
    void shouldRegisterRiderSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@test.com");
        request.setPhone("+2348011111111");
        request.setPassword("password123");
        request.setRole(Role.RIDER);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Registration successful");
        assertThat(response.getBody()).contains("accessToken");
        assertThat(response.getBody()).contains("refreshToken");
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@test.com");
        request.setPhone("+2348011111111");
        request.setPassword("password123");
        request.setRole(Role.RIDER);

        restTemplate.postForEntity("/api/v1/auth/register", request, String.class);

        // Second registration with same email
        request.setPhone("+2348022222222");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Email already registered");
    }

    @Test
    @DisplayName("Should login successfully after registration")
    void shouldLoginSuccessfully() {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@test.com");
        registerRequest.setPhone("+2348011111111");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.RIDER);
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

        // Then login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@test.com");
        loginRequest.setPassword("password123");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Login successful");
        assertThat(response.getBody()).contains("accessToken");
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void shouldRejectWrongPassword() {
        // Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@test.com");
        registerRequest.setPhone("+2348011111111");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.RIDER);
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

        // Wrong password login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@test.com");
        loginRequest.setPassword("wrongpassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should reject unauthenticated access to protected endpoint")
    void shouldRejectUnauthenticatedAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/trips/my-trips", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}