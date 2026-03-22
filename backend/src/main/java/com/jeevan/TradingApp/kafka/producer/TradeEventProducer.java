package com.jeevan.TradingApp.kafka.producer;

import com.jeevan.TradingApp.kafka.config.KafkaConfig;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TradeEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a trade event after a successful BUY or SELL execution.
     * Uses orderId as the Kafka message key for partition affinity.
     */
    public void publish(TradeEvent event) {
        String key = String.valueOf(event.getOrderId());
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.TOPIC_TRADE_EVENTS, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[TradeEventProducer] Failed to send event {}: {}", event.getEventId(), ex.getMessage());
            } else {
                log.info("[TradeEventProducer] Sent event {} to partition {} offset {}",
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
