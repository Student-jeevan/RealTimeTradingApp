package com.jeevan.TradingApp.analytics.consumer;

import com.jeevan.TradingApp.analytics.service.AnalyticsService;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes transaction-events for analytics cash-flow tracking.
 *
 * Separate consumer group (analytics-txn-group) from existing TransactionConsumer.
 *
 * Tracks:
 *   - Total deposits (CREDIT events)
 *   - Total withdrawals (DEBIT events)
 */
@Service
public class AnalyticsTransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsTransactionConsumer.class);
    private static final String CONSUMER_GROUP = "analytics-txn-group";

    @Autowired
    private AnalyticsService analyticsService;

    @KafkaListener(topics = "transaction-events", groupId = CONSUMER_GROUP)
    public void consume(TransactionEvent event) {
        log.info("[AnalyticsTxnConsumer] Received: eventId={}, type={}, amount={}",
                event.getEventId(), event.getTransactionType(), event.getAmount());

        // Idempotency check
        if (analyticsService.isEventProcessed(event.getEventId())) {
            log.warn("[AnalyticsTxnConsumer] Duplicate event {}, skipping.", event.getEventId());
            return;
        }

        try {
            analyticsService.processTransactionEvent(event);
            analyticsService.markEventProcessed(event.getEventId(), CONSUMER_GROUP);
            log.info("[AnalyticsTxnConsumer] Successfully processed event {}", event.getEventId());
        } catch (Exception e) {
            log.error("[AnalyticsTxnConsumer] Error processing event {}: {}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }
}
