package com.jeevan.TradingApp.unit.service;

import com.jeevan.TradingApp.service.MarketDataCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketDataCacheService.
 *
 * Redis interactions are fully mocked — no real Redis server needed.
 * Tests verify key naming conventions, TTL setting, and data shapes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataCacheService Unit Tests")
class MarketDataCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @InjectMocks
    private MarketDataCacheService cacheService;

    private static final String COIN_ID  = "bitcoin";
    private static final String CACHE_KEY = "market:price:bitcoin";

    @BeforeEach
    void setUp() {
        // No global stubs — each test mocks only what it needs
    }

    // =========================================================================
    //  setPrice
    // =========================================================================
    @Nested
    @DisplayName("setPrice")
    class SetPrice {

        @Test
        @DisplayName("Stores price data as Redis hash with correct key")
        void shouldStoreHashWithCorrectKey() {
            // Arrange
            Map<String, Object> priceData = buildPriceData(50_000.0);
            when(redisTemplate.opsForHash()).thenReturn(hashOps);

            // Act
            cacheService.setPrice(COIN_ID, priceData);

            // Assert — hash written and TTL set
            verify(hashOps).putAll(eq(CACHE_KEY), eq(priceData));
            verify(redisTemplate).expire(eq(CACHE_KEY), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("Sets TTL of exactly 5 minutes on every write")
        void shouldSetFiveMinuteTtl() {
            Map<String, Object> priceData = buildPriceData(50_000.0);
            when(redisTemplate.opsForHash()).thenReturn(hashOps);

            cacheService.setPrice(COIN_ID, priceData);

            // Verify expire is called (TTL enforcement)
            verify(redisTemplate, times(1)).expire(eq(CACHE_KEY), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("Overwrites stale data on repeated writes (idempotent)")
        void shouldOverwritePriceOnSubsequentCalls() {
            Map<String, Object> first  = buildPriceData(49_000.0);
            Map<String, Object> second = buildPriceData(51_000.0);
            when(redisTemplate.opsForHash()).thenReturn(hashOps);

            cacheService.setPrice(COIN_ID, first);
            cacheService.setPrice(COIN_ID, second);

            // putAll called twice — second call overwrites first
            verify(hashOps, times(2)).putAll(eq(CACHE_KEY), any());
        }
    }

    // =========================================================================
    //  getPrice
    // =========================================================================
    @Nested
    @DisplayName("getPrice")
    class GetPrice {

        @Test
        @DisplayName("Returns populated map when cache hit")
        void shouldReturnPriceMap_whenCacheHit() {
            // Arrange
            Map<Object, Object> rawRedisData = Map.of(
                    "coinId", COIN_ID,
                    "currentPrice", 50_000.0,
                    "symbol", "btc"
            );
            when(redisTemplate.opsForHash()).thenReturn(hashOps);
            when(hashOps.entries(CACHE_KEY)).thenReturn(rawRedisData);

            // Act
            Map<String, Object> result = cacheService.getPrice(COIN_ID);

            // Assert
            assertThat(result).isNotNull()
                    .containsKey("coinId")
                    .containsKey("currentPrice");
            assertThat(result.get("currentPrice")).isEqualTo(50_000.0);
        }

        @Test
        @DisplayName("Returns null when cache miss (TTL expired or never set)")
        void shouldReturnNull_whenCacheMiss() {
            // Arrange — empty map simulates no data / expired key
            when(redisTemplate.opsForHash()).thenReturn(hashOps);
            when(hashOps.entries(CACHE_KEY)).thenReturn(Map.of());

            // Act
            Map<String, Object> result = cacheService.getPrice(COIN_ID);

            // Assert — caller must handle null as cache miss
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Converts Object keys from Redis hash to String keys in result")
        void shouldConvertObjectKeysToStringKeys() {
            Map<Object, Object> rawData = Map.of(
                    "currentPrice", 70_000.0,
                    "symbol", "btc"
            );
            when(redisTemplate.opsForHash()).thenReturn(hashOps);
            when(hashOps.entries(CACHE_KEY)).thenReturn(rawData);

            Map<String, Object> result = cacheService.getPrice(COIN_ID);

            // All keys must be Strings — no ClassCastException downstream
            assertThat(result.keySet()).allMatch(k -> k instanceof String);
        }
    }

    // =========================================================================
    //  getAllPrices
    // =========================================================================
    @Nested
    @DisplayName("getAllPrices")
    class GetAllPrices {

        @Test
        @DisplayName("Returns list of all cached prices when multiple coins cached")
        void shouldReturnAllCachedPrices_whenMultipleCoinsInRedis() {
            // Arrange — Redis key pattern scan returns 2 coins
            Set<String> keys = Set.of("market:price:bitcoin", "market:price:ethereum");
            when(redisTemplate.keys("market:price:*")).thenReturn(keys);
            when(redisTemplate.opsForHash()).thenReturn(hashOps);

            when(hashOps.entries("market:price:bitcoin")).thenReturn(
                    Map.of("coinId", "bitcoin", "currentPrice", 50_000.0));
            when(hashOps.entries("market:price:ethereum")).thenReturn(
                    Map.of("coinId", "ethereum", "currentPrice", 3_000.0));

            // Act
            List<Map<String, Object>> result = cacheService.getAllPrices();

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Returns empty list when Redis has no price keys")
        void shouldReturnEmptyList_whenNoPricesInRedis() {
            when(redisTemplate.keys("market:price:*")).thenReturn(Set.of());

            List<Map<String, Object>> result = cacheService.getAllPrices();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Returns empty list when keys() returns null (Redis disconnected)")
        void shouldReturnEmptyList_whenKeysReturnsNull() {
            when(redisTemplate.keys("market:price:*")).thenReturn(null);

            List<Map<String, Object>> result = cacheService.getAllPrices();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Skips keys with empty hash data (expired mid-scan)")
        void shouldSkipExpiredKeysWithEmptyHashData() {
            Set<String> keys = Set.of("market:price:bitcoin", "market:price:expired-coin");
            when(redisTemplate.keys("market:price:*")).thenReturn(keys);
            when(redisTemplate.opsForHash()).thenReturn(hashOps);

            when(hashOps.entries("market:price:bitcoin")).thenReturn(
                    Map.of("coinId", "bitcoin", "currentPrice", 50_000.0));
            // Simulate TTL expiry between scan and entries fetch
            when(hashOps.entries("market:price:expired-coin")).thenReturn(Map.of());

            List<Map<String, Object>> result = cacheService.getAllPrices();

            // Only 1 valid result — expired key skipped
            assertThat(result).hasSize(1);
        }
    }

    // =========================================================================
    //  hasPriceInCache
    // =========================================================================
    @Nested
    @DisplayName("hasPriceInCache")
    class HasPriceInCache {

        @Test
        @DisplayName("Returns true when Redis has the key")
        void shouldReturnTrue_whenKeyExists() {
            when(redisTemplate.hasKey(CACHE_KEY)).thenReturn(true);
            assertThat(cacheService.hasPriceInCache(COIN_ID)).isTrue();
        }

        @Test
        @DisplayName("Returns false when Redis key is absent (expired or never set)")
        void shouldReturnFalse_whenKeyAbsent() {
            when(redisTemplate.hasKey(CACHE_KEY)).thenReturn(false);
            assertThat(cacheService.hasPriceInCache(COIN_ID)).isFalse();
        }

        @Test
        @DisplayName("Returns false when hasKey returns null (Redis disconnected)")
        void shouldReturnFalse_whenHasKeyReturnsNull() {
            when(redisTemplate.hasKey(CACHE_KEY)).thenReturn(null);
            assertThat(cacheService.hasPriceInCache(COIN_ID)).isFalse();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildPriceData(double price) {
        return Map.of(
                "coinId", COIN_ID,
                "symbol", "btc",
                "name", "Bitcoin",
                "currentPrice", price,
                "priceChange24h", 100.0,
                "priceChangePercentage24h", 0.2,
                "marketCap", 900_000_000_000L,
                "totalVolume", 30_000_000_000L,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
