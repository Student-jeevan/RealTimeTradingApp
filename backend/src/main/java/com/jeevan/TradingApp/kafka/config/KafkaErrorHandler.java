package com.jeevan.TradingApp.kafka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Global error handler for Kafka consumers.
 * - Retries 3 times with 1-second back-off
 * - On exhaustion, publishes to Dead Letter Topic (original-topic.DLT)
 */
@Configuration
public class KafkaErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandler.class);

    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1000L, 3) // 1 sec delay, 3 retries
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("[KafkaRetry] Attempt {} for topic={} key={}: {}",
                        deliveryAttempt, record.topic(), record.key(), ex.getMessage()));

        return errorHandler;
    }
}
