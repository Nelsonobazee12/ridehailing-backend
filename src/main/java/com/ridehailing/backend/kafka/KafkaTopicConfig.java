package com.ridehailing.backend.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TRIP_REQUESTED_TOPIC = "trip.requested";
    public static final String TRIP_ACCEPTED_TOPIC = "trip.accepted";
    public static final String TRIP_COMPLETED_TOPIC = "trip.completed";
    public static final String TRIP_CANCELLED_TOPIC = "trip.cancelled";

    @Bean
    public NewTopic tripRequestedTopic() {
        return TopicBuilder.name(TRIP_REQUESTED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tripAcceptedTopic() {
        return TopicBuilder.name(TRIP_ACCEPTED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tripCompletedTopic() {
        return TopicBuilder.name(TRIP_COMPLETED_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tripCancelledTopic() {
        return TopicBuilder.name(TRIP_CANCELLED_TOPIC).partitions(3).replicas(1).build();
    }
}