package com.ridehailing.backend.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsRequest {

    @JsonProperty("to")
    private String to;

    @JsonProperty("from")
    private String from;

    @JsonProperty("sms")
    private String sms;

    @JsonProperty("type")
    @Builder.Default
    private String type = "plain";

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("channel")
    @Builder.Default
    private String channel = "generic";
}