package com.jeevan.TradingApp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.kafka.producer.PriceUpdateProducer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket client that connects to Binance's real-time mini-ticker stream.
 *
 * Stream URL: wss://stream.binance.com:9443/ws/!miniTicker@arr
 * This broadcasts all symbols' price updates every ~1 second.
 *
 * We filter to a set of tracked symbols (BTC, ETH, etc.) and publish
 * each update as a PriceUpdateEvent to Kafka.
 */
public class BinanceWebSocketClient extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketClient.class);

    private final PriceUpdateProducer priceUpdateProducer;
    private final ObjectMapper objectMapper;
    private final Set<String> trackedSymbols;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_DELAY_SECONDS = 60;

    public BinanceWebSocketClient(URI serverUri,
                                   PriceUpdateProducer priceUpdateProducer,
                                   ObjectMapper objectMapper,
                                   Set<String> trackedSymbols) {
        super(serverUri);
        this.priceUpdateProducer = priceUpdateProducer;
        this.objectMapper = objectMapper;
        this.trackedSymbols = trackedSymbols;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        connected.set(true);
        reconnectAttempts = 0;
        log.info("[BinanceWSClient] ✅ Connected to Binance WebSocket stream");
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // The !miniTicker@arr stream sends a JSON array
            if (root.isArray()) {
                for (JsonNode ticker : root) {
                    processTicker(ticker);
                }
            } else {
                processTicker(root);
            }
        } catch (Exception e) {
            log.error("[BinanceWSClient] Error parsing message: {}", e.getMessage());
        }
    }

    private void processTicker(JsonNode ticker) {
        try {
            String symbol = ticker.get("s").asText().toLowerCase(); // e.g., "btcusdt"

            // Only process tracked symbols
            if (!trackedSymbols.contains(symbol)) {
                return;
            }

            // Map Binance symbol to our coinId (strip "usdt" suffix)
            String coinId = symbol.replace("usdt", "");

            double currentPrice = ticker.get("c").asDouble();     // close price
            double priceChange = ticker.get("p") != null ? ticker.get("p").asDouble() : 0;
            double volumeQuote = ticker.get("q") != null ? ticker.get("q").asDouble() : 0;

            PriceUpdateEvent event = PriceUpdateEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .coinId(coinId)
                    .coinSymbol(coinId)
                    .coinName(coinId.toUpperCase())
                    .currentPrice(currentPrice)
                    .priceChange24h(priceChange)
                    .priceChangePercentage24h(0)  // mini ticker doesn't provide percentage
                    .marketCap(0)
                    .totalVolume((long) volumeQuote)
                    .build();

            priceUpdateProducer.publish(event);
        } catch (Exception e) {
            log.error("[BinanceWSClient] Error processing ticker: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected.set(false);
        log.warn("[BinanceWSClient] ❌ Disconnected: code={}, reason={}, remote={}", code, reason, remote);
        scheduleReconnect();
    }

    @Override
    public void onError(Exception ex) {
        log.error("[BinanceWSClient] Error: {}", ex.getMessage());
        if (!connected.get()) {
            scheduleReconnect();
        }
    }

    /**
     * Reconnect with exponential backoff: 1s, 2s, 4s, 8s, ... up to 60s.
     */
    private void scheduleReconnect() {
        int delay = Math.min((int) Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY_SECONDS);
        reconnectAttempts++;
        log.info("[BinanceWSClient] Reconnecting in {} seconds (attempt {})...", delay, reconnectAttempts);

        reconnectExecutor.schedule(() -> {
            try {
                this.reconnect();
            } catch (Exception e) {
                log.error("[BinanceWSClient] Reconnect failed: {}", e.getMessage());
            }
        }, delay, TimeUnit.SECONDS);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void shutdown() {
        reconnectExecutor.shutdownNow();
        this.close();
    }
}
