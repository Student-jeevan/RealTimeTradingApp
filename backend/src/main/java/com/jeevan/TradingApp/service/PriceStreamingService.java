package com.jeevan.TradingApp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.kafka.producer.PriceUpdateProducer;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.repository.CoinRepository;
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

/**
 * Periodically fetches live prices from CoinGecko and publishes them
 * to the Kafka "price-updates" topic. Consumers handle WebSocket push
 * and price alert checks.
 */
@Service
public class PriceStreamingService {

    private static final Logger log = LoggerFactory.getLogger(PriceStreamingService.class);

    @Autowired
    private PriceUpdateProducer priceUpdateProducer;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Runs every 15 seconds. Fetches top 10 coins from CoinGecko
     * and publishes each as a PriceUpdateEvent to Kafka.
     */
    @Scheduled(fixedDelay = 15000)
    public void streamPrices() {
        try {
            String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&per_page=10&page=1";
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            List<Coin> coins = objectMapper.readValue(response.getBody(), new TypeReference<List<Coin>>() {});

            for (Coin coin : coins) {
                // Update DB with latest price
                coinRepository.save(coin);

                // Publish to Kafka
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

            log.info("[PriceStreamingService] Published prices for {} coins", coins.size());
        } catch (Exception e) {
            log.error("[PriceStreamingService] Error fetching/publishing prices: {}", e.getMessage());
        }
    }
}
