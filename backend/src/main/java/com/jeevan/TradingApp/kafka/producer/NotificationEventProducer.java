package com.jeevan.TradingApp.kafka.producer;

import com.jeevan.TradingApp.kafka.config.KafkaConfig;
import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a notification event. Uses userId as key.
     */
    public void publish(NotificationEvent event) {
        String key = String.valueOf(event.getUserId());
        kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATION_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[NotificationProducer] Failed for user {}: {}", event.getUserId(), ex.getMessage());
                    } else {
                        log.info("[NotificationProducer] Queued {} notification for user {}",
                                event.getType(), event.getUserId());
                    }
                });
    }
}
