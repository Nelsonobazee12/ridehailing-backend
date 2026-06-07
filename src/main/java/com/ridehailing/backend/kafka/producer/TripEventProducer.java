package com.ridehailing.backend.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridehailing.backend.kafka.KafkaTopicConfig;
import com.ridehailing.backend.model.dto.response.TripResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishTripRequested(TripResponse trip) {
        publish(KafkaTopicConfig.TRIP_REQUESTED_TOPIC, trip);
    }

    public void publishTripAccepted(TripResponse trip) {
        publish(KafkaTopicConfig.TRIP_ACCEPTED_TOPIC, trip);
    }

    public void publishTripCompleted(TripResponse trip) {
        publish(KafkaTopicConfig.TRIP_COMPLETED_TOPIC, trip);
    }

    public void publishTripCancelled(TripResponse trip) {
        publish(KafkaTopicConfig.TRIP_CANCELLED_TOPIC, trip);
    }

    private void publish(String topic, TripResponse trip) {
        try {
            String payload = objectMapper.writeValueAsString(trip);
            kafkaTemplate.send(topic, trip.getId().toString(), payload);
            log.info("Published to {}: tripId={}", topic, trip.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trip event for tripId={}: {}",
                    trip.getId(), e.getMessage());
        }
    }

    public void publishTripStatusUpdated(TripResponse trip) {
        publish(KafkaTopicConfig.TRIP_STATUS_UPDATED_TOPIC, trip);
    }
}