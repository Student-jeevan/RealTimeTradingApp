package com.jeevan.TradingApp.analytics.consumer;

import com.jeevan.TradingApp.analytics.service.AnalyticsService;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consumes price-updates for real-time portfolio valuation.
 *
 * FIX: Previously recomputed ALL tracked users on every price tick.
 * Now queries only users who hold the updated coin — dramatically reducing
 * DB load. (50 users × 100 coin updates/s was producing 5,000 recomputations/s)
 */
@Service
public class AnalyticsPriceConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsPriceConsumer.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AssetRepository assetRepository;

    @KafkaListener(topics = "price-updates", groupId = "analytics-price-group")
    public void consume(PriceUpdateEvent event) {
        log.debug("[AnalyticsPriceConsumer] Price update: {} = ${}", event.getCoinSymbol(), event.getCurrentPrice());

        try {
            // 1. Cache the price (in-memory + Redis)
            analyticsService.processPriceUpdate(event.getCoinId(), event.getCurrentPrice());

            // 2. FIX: Only recompute users who actually hold this coin
            //    Previously: getAllTrackedUserIds() → recompute ALL users (unbounded!)
            //    Now: findUserIdsByCoinId() → only affected users (bounded by coin holders)
            List<Long> affectedUserIds = assetRepository.findUserIdsByCoinId(event.getCoinId());

            if (affectedUserIds.isEmpty()) {
                log.debug("[AnalyticsPriceConsumer] No holders for {}, skipping portfolio recompute.", event.getCoinId());
                return;
            }

            log.debug("[AnalyticsPriceConsumer] Recomputing portfolio for {} holder(s) of {}",
                    affectedUserIds.size(), event.getCoinId());

            for (Long userId : affectedUserIds) {
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
