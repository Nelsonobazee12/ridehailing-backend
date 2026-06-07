package com.ridehailing.backend.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaystackInitRequest {

    @JsonProperty("email")
    private String email;

    // Paystack accepts amount in kobo (multiply naira by 100)
    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("currency")
    @Builder.Default
    private String currency = "NGN";

    @JsonProperty("metadata")
    private Object metadata;
}