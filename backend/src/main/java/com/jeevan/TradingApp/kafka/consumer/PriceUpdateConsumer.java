package com.jeevan.TradingApp.kafka.consumer;

import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.repository.PriceAlertRepository;
import com.jeevan.TradingApp.service.MarketDataCacheService;
import com.jeevan.TradingApp.service.PriceAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consumes price update events from Kafka and performs 3 actions:
 *
 * 1. Updates Redis hot cache (2s TTL) for fast REST reads
 * 2. Pushes live prices to WebSocket subscribers (per-coin topics)
 * 3. Checks user price alerts against the new price
 */
@Service
public class PriceUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateConsumer.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MarketDataCacheService cacheService;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private PriceAlertService priceAlertService;

    @KafkaListener(topics = "price-updates", groupId = "price-group")
    public void consume(PriceUpdateEvent event) {
        log.debug("[PriceUpdateConsumer] {} @ ${}", event.getCoinId(), event.getCurrentPrice());

        try {
            // Build price data map (shared across cache, WS, and any future consumers)
            Map<String, Object> priceData = new HashMap<>();
            priceData.put("coinId", event.getCoinId());
            priceData.put("symbol", event.getCoinSymbol());
            priceData.put("name", event.getCoinName());
            priceData.put("currentPrice", event.getCurrentPrice());
            priceData.put("priceChange24h", event.getPriceChange24h());
            priceData.put("priceChangePercentage24h", event.getPriceChangePercentage24h());
            priceData.put("marketCap", event.getMarketCap());
            priceData.put("totalVolume", event.getTotalVolume());
            priceData.put("timestamp", event.getTimestamp().toString());

            // 1. Update Redis hot cache (2-second TTL)
            cacheService.setPrice(event.getCoinId(), priceData);

            // 2. Push to WebSocket — per-coin topic for frontend subscriptions
            messagingTemplate.convertAndSend("/topic/prices/" + event.getCoinId(), priceData);

            // Also broadcast to a global prices topic for dashboard views
            messagingTemplate.convertAndSend("/topic/prices", priceData);

            // 3. Check price alerts for this coin
            checkPriceAlerts(event);

        } catch (Exception e) {
            log.error("[PriceUpdateConsumer] Error for {}: {}", event.getCoinId(), e.getMessage(), e);
        }
    }

    private void checkPriceAlerts(PriceUpdateEvent event) {
        try {
            List<PriceAlert> activeAlerts = priceAlertRepository.findByCoinAndTriggeredFalse(event.getCoinId());
            if (activeAlerts.isEmpty()) return;

            BigDecimal currentPrice = BigDecimal.valueOf(event.getCurrentPrice());

            for (PriceAlert alert : activeAlerts) {
                boolean met = switch (alert.getAlertCondition()) {
                    case ABOVE -> currentPrice.compareTo(alert.getTargetPrice()) >= 0;
                    case BELOW -> currentPrice.compareTo(alert.getTargetPrice()) <= 0;
                };

                if (met) {
                    priceAlertService.triggerAlert(alert, currentPrice);
                    log.info("[PriceUpdateConsumer] Alert {} triggered for {} @ ${}",
                            alert.getId(), event.getCoinId(), currentPrice);
                }
            }
        } catch (Exception e) {
            log.warn("[PriceUpdateConsumer] Alert check failed for {}: {}", event.getCoinId(), e.getMessage());
        }
    }
}
