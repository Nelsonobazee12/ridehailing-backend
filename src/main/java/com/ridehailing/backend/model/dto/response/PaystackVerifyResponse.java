package com.ridehailing.backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaystackVerifyResponse {

    @JsonProperty("status")
    private Boolean status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private PaystackVerifyData data;

    @Data
    public static class PaystackVerifyData {

        @JsonProperty("status")
        private String status;

        @JsonProperty("reference")
        private String reference;

        @JsonProperty("amount")
        private Long amount;

        @JsonProperty("channel")
        private String channel;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("paid_at")
        private String paidAt;

        @JsonProperty("gateway_response")
        private String gatewayResponse;

        @JsonProperty("customer")
        private Customer customer;

        @Data
        public static class Customer {
            @JsonProperty("email")
            private String email;
        }
    }
}