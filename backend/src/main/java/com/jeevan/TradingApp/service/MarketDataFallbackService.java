package com.jeevan.TradingApp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.kafka.producer.PriceUpdateProducer;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.repository.CoinRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CoinGecko REST fallback — only active when the Binance WebSocket is disconnected.
 * Protected by a Resilience4j circuit breaker to avoid hammering during rate limits.
 */
@Service
public class MarketDataFallbackService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataFallbackService.class);

    private final AtomicBoolean active = new AtomicBoolean(false);

    @Autowired
    private PriceUpdateProducer priceUpdateProducer;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    public void setActive(boolean isActive) {
        if (this.active.get() != isActive) {
            this.active.set(isActive);
            log.info("[CoinGecko Fallback] {}", isActive ? "ACTIVATED" : "DEACTIVATED");
        }
    }

    public boolean isActive() {
        return active.get();
    }

    /**
     * Polls CoinGecko every 5 seconds — but only when active (Binance is down).
     */
    @Scheduled(fixedDelay = 5000)
    public void pollPrices() {
        if (!active.get()) {
            return; // Binance is handling it
        }
        fetchAndPublish();
    }

    @CircuitBreaker(name = "coingecko", fallbackMethod = "onCoinGeckoFailure")
    public void fetchAndPublish() {
        try {
            String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&per_page=15&page=1";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            List<Coin> coins = objectMapper.readValue(response.getBody(), new TypeReference<List<Coin>>() {});

            for (Coin coin : coins) {
                coinRepository.save(coin);

                PriceUpdateEvent event = PriceUpdateEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .timestamp(LocalDateTime.now())
                        .coinId(coin.getId())
                        .coinSymbol(coin.getSymbol())
                        .coinName(coin.getName())
                        .currentPrice(coin.getCurrentPrice())
                        .priceChange24h(coin.getPriceChange24h())
                        .priceChangePercentage24h(coin.getPriceChangePercentage24h())
                        .marketCap(coin.getMarketCap())
                        .totalVolume(coin.getTotalVolume())
                        .build();

                priceUpdateProducer.publish(event);
            }

            log.info("[CoinGecko Fallback] Published prices for {} coins", coins.size());
        } catch (Exception e) {
            log.error("[CoinGecko Fallback] Error: {}", e.getMessage());
            throw new RuntimeException(e); // Let circuit breaker handle it
        }
    }

    /**
     * Circuit breaker fallback — called when CoinGecko is rate-limited or down.
     */
    @SuppressWarnings("unused")
    private void onCoinGeckoFailure(Throwable t) {
        log.warn("[CoinGecko Fallback] Circuit breaker OPEN — skipping poll: {}", t.getMessage());
    }
}
