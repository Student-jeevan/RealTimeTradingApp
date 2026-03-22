package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.exception.CustomException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.repository.CoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Coin service — refactored to read live prices from Redis cache first.
 *
 * - getCoinList() → reads from MarketDataCacheService (sub-second data)
 * - getCoinDetails() → still calls CoinGecko for full metadata (cached separately)
 * - findById() → DB lookup (for trade execution, needs the Coin entity)
 */
@Service
public class CoinServiceImpl implements CoinService {
    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketDataCacheService cacheService;

    /**
     * Returns coin list from Redis hot cache first.
     * Falls back to CoinGecko REST if cache is empty (cold start).
     */
    @Override
    public List<Coin> getCoinList(int page) {
        // Try Redis cache first (sub-second freshness)
        List<Map<String, Object>> cachedPrices = cacheService.getAllPrices();
        if (!cachedPrices.isEmpty()) {
            // Map cached data back to Coin entities from DB (enriched with full metadata)
            List<Coin> coins = new ArrayList<>();
            for (Map<String, Object> cached : cachedPrices) {
                String coinId = cached.get("coinId").toString();
                Optional<Coin> dbCoin = coinRepository.findById(coinId);
                if (dbCoin.isPresent()) {
                    Coin coin = dbCoin.get();
                    // Override price with real-time cached price
                    coin.setCurrentPrice(((Number) cached.get("currentPrice")).doubleValue());
                    coins.add(coin);
                }
            }
            if (!coins.isEmpty()) {
                // Sort by market cap rank (ascending) and paginate
                coins.sort(Comparator.comparingInt(Coin::getMarketCapRank));
                int start = (page - 1) * 10;
                int end = Math.min(start + 10, coins.size());
                if (start < coins.size()) {
                    return coins.subList(start, end);
                }
            }
        }

        // Fallback: direct CoinGecko call (only during cold start)
        return fetchCoinListFromCoinGecko(page);
    }

    private List<Coin> fetchCoinListFromCoinGecko(int page) {
        String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&per_page=10&page=" + page;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            try {
                return objectMapper.readValue(response.getBody(), new TypeReference<List<Coin>>() {
                });
            } catch (Exception e) {
                throw new CustomException("Error parsing coin list", "JSON_ERROR");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CustomException(e.getMessage(), "API_ERROR");
        }
    }

    @Override
    public String getMarketChart(String coinId, int days) {
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId + "/market_chart?vs_currency=usd&days=" + days;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CustomException(e.getMessage(), "API_ERROR");
        }
    }

    @Override
    public String getCoinDetails(String coinId) {
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                Coin coin = new Coin();
                coin.setId(jsonNode.get("id").asText());
                coin.setName(jsonNode.get("name").asText());
                coin.setSymbol(jsonNode.get("symbol").asText());
                coin.setImage(jsonNode.get("image").get("large").asText());
                JsonNode marketData = jsonNode.get("market_data");
                coin.setCurrentPrice(marketData.get("current_price").get("usd").asDouble());
                coin.setMarketCap(marketData.get("market_cap").get("usd").asLong());
                coin.setMarketCapRank(jsonNode.get("market_cap_rank").asInt());
                coin.setTotalVolume(marketData.get("total_volume").get("usd").asLong());
                coin.setLow24h(marketData.get("low_24h").get("usd").asDouble());
                coin.setHigh24h(marketData.get("high_24h").get("usd").asDouble());
                coin.setPriceChange24h(marketData.get("price_change_24h").asDouble());
                coin.setPriceChangePercentage24h(marketData.get("price_change_percentage_24h").asDouble());
                coin.setMarketCapChange24h(marketData.get("market_cap_change_24h").asLong());
                coin.setMarketCapChangePercentage24h(marketData.get("market_cap_change_percentage_24h").asDouble());
                coin.setTotalSupply(marketData.get("total_supply").asLong());
                coinRepository.save(coin);
                return response.getBody();
            } catch (Exception e) {
                throw new CustomException("Error parsing coin details", "JSON_ERROR");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CustomException(e.getMessage(), "API_ERROR");
        }
    }

    @Override
    public Coin findById(String coinId) {
        Optional<Coin> optionalCoin = coinRepository.findById(coinId);
        if (optionalCoin.isEmpty()) {
            throw new CustomException("coin not found", "COIN_NOT_FOUND");
        }
        return optionalCoin.get();
    }

    @Override
    public String searchCoin(String keyword) {
        String url = "https://api.coingecko.com/api/v3/search?query=" + keyword;
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CustomException(e.getMessage(), "API_ERROR");
        }
    }

    @Override
    public String getTop50CoinsByMarketCapRank() {
        String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&per_page=50&page=1";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CustomException(e.getMessage(), "API_ERROR");
        }
    }

    @Override
    public String getTreadingCoins() {
        String url = "https://api.coingecko.com/api/v3/search/trending";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new CustomException(e.getMessage(), "API_ERROR");
        }
    }
}
