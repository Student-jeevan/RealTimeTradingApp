package com.jeevan.TradingApp.kafka.producer;

import com.jeevan.TradingApp.kafka.config.KafkaConfig;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PriceUpdateProducer {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a price update event. Uses coinId as key so all updates
     * for the same coin go to the same partition (ordering guarantee).
     */
    public void publish(PriceUpdateEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC_PRICE_UPDATES, event.getCoinId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PriceUpdateProducer] Failed for coin {}: {}", event.getCoinId(), ex.getMessage());
                    } else {
                        log.debug("[PriceUpdateProducer] Sent price for {} @ ${}",
                                event.getCoinId(), event.getCurrentPrice());
                    }
                });
    }
}
