package com.ridehailing.backend.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SmsResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("balance")
    private Double balance;

    @JsonProperty("user")
    private String user;
}