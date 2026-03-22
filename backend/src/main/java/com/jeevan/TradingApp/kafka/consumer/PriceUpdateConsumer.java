package com.jeevan.TradingApp.kafka.consumer;

import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.repository.PriceAlertRepository;
import com.jeevan.TradingApp.service.PriceAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Consumes price update events from Kafka.
 * - Pushes live prices to all connected WebSocket clients
 * - Checks user price alerts against the new price
 */
@Service
public class PriceUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateConsumer.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private PriceAlertService priceAlertService;

    @KafkaListener(topics = "price-updates", groupId = "price-group")
    public void consume(PriceUpdateEvent event) {
        log.debug("[PriceUpdateConsumer] Received price for {} @ ${}", event.getCoinId(), event.getCurrentPrice());

        try {
            // 1. Push to WebSocket for live frontend updates
            Map<String, Object> priceData = Map.of(
                    "coinId", event.getCoinId(),
                    "symbol", event.getCoinSymbol(),
                    "name", event.getCoinName(),
                    "currentPrice", event.getCurrentPrice(),
                    "priceChange24h", event.getPriceChange24h(),
                    "priceChangePercentage24h", event.getPriceChangePercentage24h(),
                    "marketCap", event.getMarketCap(),
                    "totalVolume", event.getTotalVolume(),
                    "timestamp", event.getTimestamp().toString()
            );
            messagingTemplate.convertAndSend("/topic/prices/" + event.getCoinId(), priceData);

            // 2. Check price alerts for this coin
            List<PriceAlert> activeAlerts = priceAlertRepository.findByCoinAndTriggeredFalse(event.getCoinId());
            BigDecimal currentPrice = BigDecimal.valueOf(event.getCurrentPrice());

            for (PriceAlert alert : activeAlerts) {
                boolean met = switch (alert.getAlertCondition()) {
                    case ABOVE -> currentPrice.compareTo(alert.getTargetPrice()) >= 0;
                    case BELOW -> currentPrice.compareTo(alert.getTargetPrice()) <= 0;
                };

                if (met) {
                    priceAlertService.triggerAlert(alert, currentPrice);
                    log.info("[PriceUpdateConsumer] Triggered alert {} for coin {} @ ${}",
                            alert.getId(), event.getCoinId(), currentPrice);
                }
            }
        } catch (Exception e) {
            log.error("[PriceUpdateConsumer] Error processing price for {}: {}", event.getCoinId(), e.getMessage(), e);
        }
    }
}
