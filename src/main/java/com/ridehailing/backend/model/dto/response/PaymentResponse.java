package com.ridehailing.backend.model.dto.response;

import com.ridehailing.backend.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long tripId;
    private Long riderId;
    private String reference;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private String authorizationUrl;
    private String channel;
    private String paidAt;
    private LocalDateTime createdAt;
}