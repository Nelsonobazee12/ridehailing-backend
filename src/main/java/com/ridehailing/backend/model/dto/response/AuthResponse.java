package com.ridehailing.backend.model.dto.response;

import com.ridehailing.backend.model.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private String accessToken;
    private String refreshToken;
}