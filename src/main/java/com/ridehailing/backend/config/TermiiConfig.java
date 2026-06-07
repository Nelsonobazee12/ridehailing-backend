package com.ridehailing.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.termii")
public class TermiiConfig {
    private String apiKey;
    private String baseUrl;
    private String senderId;
}