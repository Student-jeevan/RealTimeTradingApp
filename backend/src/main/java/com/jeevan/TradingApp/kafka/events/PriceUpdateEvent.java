package com.jeevan.TradingApp.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published periodically when coin prices are fetched from CoinGecko.
 * Consumers push to WebSocket and check price alerts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private String coinId;
    private String coinSymbol;
    private String coinName;
    private double currentPrice;
    private double priceChange24h;
    private double priceChangePercentage24h;
    private long marketCap;
    private long totalVolume;
}
