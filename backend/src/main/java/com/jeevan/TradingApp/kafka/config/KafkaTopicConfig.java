package com.jeevan.TradingApp.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates topics programmatically on startup (idempotent — won't fail if they already exist).
 * The Docker init container also creates them, so this is a safety net.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic tradeEventsTopic() {
        return TopicBuilder.name(KafkaConfig.TOPIC_TRADE_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic priceUpdatesTopic() {
        return TopicBuilder.name(KafkaConfig.TOPIC_PRICE_UPDATES)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(KafkaConfig.TOPIC_NOTIFICATION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(KafkaConfig.TOPIC_TRANSACTION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
