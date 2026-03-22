package com.jeevan.TradingApp.kafka.producer;

import com.jeevan.TradingApp.kafka.config.KafkaConfig;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a transaction event for audit logging. Uses userId as key.
     */
    public void publish(TransactionEvent event) {
        String key = String.valueOf(event.getUserId());
        kafkaTemplate.send(KafkaConfig.TOPIC_TRANSACTION_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[TransactionProducer] Failed for user {}: {}", event.getUserId(), ex.getMessage());
                    } else {
                        log.debug("[TransactionProducer] Sent {} event for user {}",
                                event.getTransactionType(), event.getUserId());
                    }
                });
    }
}
