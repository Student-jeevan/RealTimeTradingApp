package com.jeevan.TradingApp.analytics.consumer;

import com.jeevan.TradingApp.analytics.service.AnalyticsService;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes trade-events for analytics processing.
 *
 * Uses a SEPARATE consumer group (analytics-trade-group) so it does NOT
 * interfere with the existing TradeEventConsumer (trade-group).
 *
 * On each FILLED trade:
 *   - Records the trade in trade_analytics
 *   - Updates aggregate metrics in user_analytics (PnL, win/loss counters)
 */
@Service
public class AnalyticsTradeConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsTradeConsumer.class);
    private static final String CONSUMER_GROUP = "analytics-trade-group";

    @Autowired
    private AnalyticsService analyticsService;

    @KafkaListener(topics = "trade-events", groupId = CONSUMER_GROUP)
    public void consume(TradeEvent event) {
        log.info("[AnalyticsTradeConsumer] Received: eventId={}, orderId={}, type={}",
                event.getEventId(), event.getOrderId(), event.getOrderType());

        // Idempotency check
        if (analyticsService.isEventProcessed(event.getEventId())) {
            log.warn("[AnalyticsTradeConsumer] Duplicate event {}, skipping.", event.getEventId());
            return;
        }

        try {
            analyticsService.processTradeEvent(event);
            analyticsService.markEventProcessed(event.getEventId(), CONSUMER_GROUP);
            log.info("[AnalyticsTradeConsumer] Successfully processed event {}", event.getEventId());
        } catch (Exception e) {
            log.error("[AnalyticsTradeConsumer] Error processing event {}: {}",
                    event.getEventId(), e.getMessage(), e);
            throw e; // let error handler retry
        }
    }
}
