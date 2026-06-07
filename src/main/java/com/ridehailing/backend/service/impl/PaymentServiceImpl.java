package com.ridehailing.backend.service.impl;

import com.ridehailing.backend.exception.AppException;
import com.ridehailing.backend.model.dto.request.PaystackInitRequest;
import com.ridehailing.backend.model.dto.response.PaymentResponse;
import com.ridehailing.backend.model.dto.response.PaystackInitResponse;
import com.ridehailing.backend.model.dto.response.PaystackVerifyResponse;
import com.ridehailing.backend.model.entity.Payment;
import com.ridehailing.backend.model.entity.Trip;
import com.ridehailing.backend.model.entity.User;
import com.ridehailing.backend.model.enums.PaymentStatus;
import com.ridehailing.backend.model.enums.TripStatus;
import com.ridehailing.backend.repository.PaymentRepository;
import com.ridehailing.backend.repository.TripRepository;
import com.ridehailing.backend.repository.UserRepository;
import com.ridehailing.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final PaystackClient paystackClient;

    @Override
    @Transactional
    public PaymentResponse initializePayment(Long tripId, String riderEmail) {
        User rider = userRepository.findByEmail(riderEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new AppException("Trip not found", HttpStatus.NOT_FOUND));

        // Validate ownership
        if (!trip.getRider().getId().equals(rider.getId())) {
            throw new AppException("This trip does not belong to you", HttpStatus.FORBIDDEN);
        }

        // Only allow payment for completed trips
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new AppException("Payment can only be made for completed trips",
                    HttpStatus.BAD_REQUEST);
        }

        // Prevent duplicate payment
        if (paymentRepository.existsByTripIdAndStatus(tripId, PaymentStatus.PAID)) {
            throw new AppException("Trip has already been paid for", HttpStatus.CONFLICT);
        }

        // Check if pending payment already exists — reuse it
        Payment payment = paymentRepository.findByTripId(tripId)
                .orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            log.info("Reusing existing payment reference {} for trip {}",
                    payment.getReference(), tripId);
            return toResponse(payment);
        }

        // Generate unique reference
        String reference = "RH-" + tripId + "-" + UUID.randomUUID().toString().substring(0, 8)
                .toUpperCase();

        // Amount in kobo
        long amountInKobo = (long) (trip.getActualFare() * 100);

        PaystackInitRequest initRequest = PaystackInitRequest.builder()
                .email(rider.getEmail())
                .amount(amountInKobo)
                .reference(reference)
                .currency("NGN")
                .build();

        PaystackInitResponse paystackResponse = paystackClient.initializeTransaction(initRequest);

        if (paystackResponse == null || !Boolean.TRUE.equals(paystackResponse.getStatus())) {
            throw new AppException("Failed to initialize payment with Paystack",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        payment = Payment.builder()
                .trip(trip)
                .rider(rider)
                .reference(reference)
                .amount(trip.getActualFare())
                .currency("NGN")
                .status(PaymentStatus.PENDING)
                .paystackAccessCode(paystackResponse.getData().getAccessCode())
                .authorizationUrl(paystackResponse.getData().getAuthorizationUrl())
                .build();

        paymentRepository.save(payment);

        // Save reference on trip
        trip.setPaystackReference(reference);
        tripRepository.save(trip);

        log.info("Payment initialized for trip {}: reference={}", tripId, reference);
        return toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return toResponse(payment);
        }

        PaystackVerifyResponse verifyResponse = paystackClient.verifyTransaction(reference);

        if (verifyResponse == null || verifyResponse.getData() == null) {
            throw new AppException("Could not verify payment with Paystack",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String paystackStatus = verifyResponse.getData().getStatus();

        if ("success".equals(paystackStatus)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setChannel(verifyResponse.getData().getChannel());
            payment.setPaidAt(verifyResponse.getData().getPaidAt());
            payment.setGatewayResponse(verifyResponse.getData().getGatewayResponse());

            // Update trip payment status
            Trip trip = payment.getTrip();
            trip.setPaymentStatus(PaymentStatus.PAID);
            tripRepository.save(trip);

            log.info("Payment verified for reference {}: PAID", reference);
        } else if ("failed".equals(paystackStatus)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(verifyResponse.getData().getGatewayResponse());
            log.warn("Payment failed for reference {}", reference);
        }

        paymentRepository.save(payment);
        return toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByTrip(Long tripId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Payment payment = paymentRepository.findByTripId(tripId)
                .orElseThrow(() -> new AppException("Payment not found for this trip",
                        HttpStatus.NOT_FOUND));

        boolean isRider = payment.getRider().getId().equals(user.getId());
        boolean isDriver = payment.getTrip().getDriver() != null &&
                payment.getTrip().getDriver().getId().equals(user.getId());

        if (!isRider && !isDriver) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tripId(payment.getTrip().getId())
                .riderId(payment.getRider().getId())
                .reference(payment.getReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .authorizationUrl(payment.getAuthorizationUrl())
                .channel(payment.getChannel())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}