package com.jeevan.TradingApp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.kafka.producer.PriceUpdateProducer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Set;

/**
 * Central Market Data Service — the ONLY component that fetches external price data.
 *
 * Primary source:  Binance WebSocket (sub-second updates, free, no API key)
 * Fallback source: CoinGecko REST (via MarketDataFallbackService, circuit-breaker protected)
 *
 * All price data flows through Kafka "price-updates" topic.
 * No user request ever calls an external API directly.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/!miniTicker@arr";

    /**
     * Tracked symbols — Binance uses lowercase pairs like "btcusdt".
     * Add more pairs here to track additional coins.
     */
    private static final Set<String> TRACKED_SYMBOLS = Set.of(
            "btcusdt", "ethusdt", "bnbusdt", "solusdt", "xrpusdt",
            "adausdt", "dogeusdt", "avaxusdt", "dotusdt", "maticusdt",
            "linkusdt", "ltcusdt", "uniusdt", "atomusdt", "xlmusdt"
    );

    @Autowired
    private PriceUpdateProducer priceUpdateProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketDataFallbackService fallbackService;

    @Value("${market.data.binance.enabled:true}")
    private boolean binanceEnabled;

    private BinanceWebSocketClient binanceClient;

    @PostConstruct
    public void init() {
        if (binanceEnabled) {
            connectToBinance();
        } else {
            log.info("[MarketDataService] Binance WS disabled — using CoinGecko fallback only");
            fallbackService.setActive(true);
        }
    }

    private void connectToBinance() {
        try {
            URI uri = new URI(BINANCE_WS_URL);
            binanceClient = new BinanceWebSocketClient(uri, priceUpdateProducer, objectMapper, TRACKED_SYMBOLS);
            binanceClient.connect();
            log.info("[MarketDataService] Connecting to Binance WebSocket...");

            // Monitor connection health in background
            startHealthMonitor();
        } catch (Exception e) {
            log.error("[MarketDataService] Failed to start Binance WS: {}. Activating fallback.", e.getMessage());
            fallbackService.setActive(true);
        }
    }

    /**
     * Periodically checks if Binance WS is still connected.
     * If disconnected for too long, activates CoinGecko fallback.
     */
    private void startHealthMonitor() {
        Thread monitor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10_000); // Check every 10 seconds
                    if (binanceClient != null && !binanceClient.isConnected()) {
                        log.warn("[MarketDataService] Binance WS disconnected — enabling CoinGecko fallback");
                        fallbackService.setActive(true);
                    } else {
                        fallbackService.setActive(false); // Binance is healthy, disable fallback
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "binance-health-monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    @PreDestroy
    public void shutdown() {
        if (binanceClient != null) {
            binanceClient.shutdown();
            log.info("[MarketDataService] Binance WS client shut down");
        }
    }

    /**
     * Returns the set of tracked symbols for informational/admin purposes.
     */
    public Set<String> getTrackedSymbols() {
        return TRACKED_SYMBOLS;
    }

    /**
     * Returns true if Binance WebSocket is currently connected.
     */
    public boolean isBinanceConnected() {
        return binanceClient != null && binanceClient.isConnected();
    }
}
