package com.jeevan.TradingApp.analytics.consumer;

import com.jeevan.TradingApp.analytics.service.AnalyticsService;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consumes price-updates for real-time portfolio valuation.
 *
 * Separate consumer group (analytics-price-group) from existing PriceUpdateConsumer.
 *
 * On each price update:
 *   1. Caches the latest price (in-memory + Redis)
 *   2. Recomputes portfolio value and unrealized PnL for all tracked users
 *   3. Pushes analytics updates via WebSocket
 *
 * NOT idempotent — price updates are overwrite-safe (latest price always wins).
 */
@Service
public class AnalyticsPriceConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsPriceConsumer.class);

    @Autowired
    private AnalyticsService analyticsService;

    @KafkaListener(topics = "price-updates", groupId = "analytics-price-group")
    public void consume(PriceUpdateEvent event) {
        log.debug("[AnalyticsPriceConsumer] Price update: {} = ${}", event.getCoinSymbol(), event.getCurrentPrice());

        try {
            // 1. Cache the price
            analyticsService.processPriceUpdate(event.getCoinId(), event.getCurrentPrice());

            // 2. Recompute portfolio for all tracked users who hold this coin
            List<Long> userIds = analyticsService.getAllTrackedUserIds();
            for (Long userId : userIds) {
                try {
                    analyticsService.recomputePortfolioForUser(userId);
                } catch (Exception e) {
                    log.warn("[AnalyticsPriceConsumer] Failed to recompute portfolio for user {}: {}",
                            userId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[AnalyticsPriceConsumer] Error processing price update for {}: {}",
                    event.getCoinId(), e.getMessage(), e);
            // Don't throw — price updates are best-effort, no need to retry
        }
    }
}
