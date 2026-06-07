package com.ridehailing.backend.controller;

import com.ridehailing.backend.model.dto.response.ApiResponse;
import com.ridehailing.backend.model.dto.response.PaymentResponse;
import com.ridehailing.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize/{tripId}")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> initializePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId) {
        PaymentResponse response = paymentService.initializePayment(
                tripId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                "Payment initialized. Complete payment via the authorization URL.", response));
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable String reference) {
        PaymentResponse response = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(ApiResponse.success("Payment verified", response));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTrip(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId) {
        PaymentResponse response = paymentService.getPaymentByTrip(
                tripId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", response));
    }
}