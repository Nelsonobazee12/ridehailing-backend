package com.ridehailing.backend.service.impl;

import com.ridehailing.backend.config.PaystackConfig;
import com.ridehailing.backend.model.dto.request.PaystackInitRequest;
import com.ridehailing.backend.model.dto.response.PaystackInitResponse;
import com.ridehailing.backend.model.dto.response.PaystackVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaystackClient {

    private final PaystackConfig paystackConfig;
    private final WebClient.Builder webClientBuilder;

    private WebClient getClient() {
        return webClientBuilder
                .baseUrl(paystackConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Bearer " + paystackConfig.getSecretKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public PaystackInitResponse initializeTransaction(PaystackInitRequest request) {
        return getClient()
                .post()
                .uri("/transaction/initialize")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaystackInitResponse.class)
                .block();
    }

    public PaystackVerifyResponse verifyTransaction(String reference) {
        return getClient()
                .get()
                .uri("/transaction/verify/" + reference)
                .retrieve()
                .bodyToMono(PaystackVerifyResponse.class)
                .block();
    }
}