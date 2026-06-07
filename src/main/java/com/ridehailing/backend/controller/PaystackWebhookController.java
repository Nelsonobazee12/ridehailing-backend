package com.ridehailing.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.backend.config.PaystackConfig;
import com.ridehailing.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class PaystackWebhookController {

    private final PaymentService paymentService;
    private final PaystackConfig paystackConfig;
    private final ObjectMapper objectMapper;

    @PostMapping("/paystack")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("x-paystack-signature") String signature,
            @RequestBody String payload) {

        // Verify webhook signature
        if (!isValidSignature(payload, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.badRequest().build();
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.get("event").asText();

            log.info("Paystack webhook received: {}", eventType);

            if ("charge.success".equals(eventType)) {
                String reference = event
                        .get("data")
                        .get("reference")
                        .asText();

                paymentService.verifyPayment(reference);
                log.info("Webhook processed: charge.success for reference {}", reference);
            }

        } catch (Exception e) {
            log.error("Error processing Paystack webhook: {}", e.getMessage());
        }

        // Always return 200 to Paystack — even on error
        // Otherwise Paystack will retry the webhook
        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    paystackConfig.getSecretKey().getBytes(), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            String expectedSignature = Hex.encodeHexString(hash);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}