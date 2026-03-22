package com.jeevan.TradingApp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based hot cache for market prices.
 *
 * Key format:  market:price:{coinId}
 * TTL:         2 seconds (auto-expires if no updates come in)
 *
 * This is the fastest read path for current prices.
 * Frontend REST endpoints read from here, not from the database.
 */
@Service
public class MarketDataCacheService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCacheService.class);

    private static final String KEY_PREFIX = "market:price:";
    private static final Duration PRICE_TTL = Duration.ofSeconds(2);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Store/update the latest price for a coin with 2-second TTL.
     */
    public void setPrice(String coinId, Map<String, Object> priceData) {
        String key = KEY_PREFIX + coinId;
        redisTemplate.opsForHash().putAll(key, priceData);
        redisTemplate.expire(key, PRICE_TTL);
    }

    /**
     * Get the latest cached price for a coin.
     * Returns null if not in cache (expired or never set).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPrice(String coinId) {
        String key = KEY_PREFIX + coinId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }

    /**
     * Get all currently cached prices.
     * Uses key pattern scan (safe for small sets of tracked coins).
     */
    public List<Map<String, Object>> getAllPrices() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> prices = new ArrayList<>();
        for (String key : keys) {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                entries.forEach((k, v) -> data.put(k.toString(), v));
                prices.add(data);
            }
        }
        return prices;
    }

    /**
     * Check if a coin's price is currently in cache.
     */
    public boolean hasPriceInCache(String coinId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + coinId));
    }
}
