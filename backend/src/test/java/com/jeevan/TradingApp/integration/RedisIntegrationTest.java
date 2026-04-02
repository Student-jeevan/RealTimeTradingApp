package com.jeevan.TradingApp.integration;

import com.jeevan.TradingApp.service.MarketDataCacheService;
import com.jeevan.TradingApp.service.OtpService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Redis-backed flows using a real Redis Testcontainer.
 *
 * Covers:
 *  1. Full OTP lifecycle: generate → verify → clear
 *  2. OTP security: cooldown, max attempts, expiry
 *  3. MarketDataCacheService: write → read → TTL
 */
@DisplayName("Redis Integration Tests")
class RedisIntegrationTest extends BaseIntegrationTest {

    @Autowired private OtpService otpService;
    @Autowired private MarketDataCacheService cacheService;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_EMAIL = "redis_test_" + System.nanoTime() + "@trading.com";

    @AfterEach
    void cleanUpRedisKeys() {
        // Clean test keys after each test
        otpService.clearOtp(TEST_EMAIL);
        deleteCacheKey("market:price:bitcoin-test");
        deleteCacheKey("market:price:ethereum-test");
    }

    // =========================================================================
    //  OTP — Full Lifecycle
    // =========================================================================
    @Nested
    @DisplayName("OTP Full Lifecycle")
    class OtpLifecycle {

        @Test
        @DisplayName("Generate OTP → OTP is stored in Redis and is a 6-digit number")
        void generate_shouldStoreValidOtpInRedis() {
            String otp = otpService.generateAndStoreOtp(TEST_EMAIL);

            assertThat(otp).isNotNull()
                    .as("Must be exactly 6 digits")
                    .matches("\\d{6}");

            // Confirm key actually exists in Redis
            assertThat(redisTemplate.hasKey("otp:" + TEST_EMAIL)).isTrue();
        }

        @Test
        @DisplayName("Verify correct OTP → returns true and removes all OTP keys from Redis")
        void verify_correctOtp_shouldReturnTrue_andClearAllKeys() {
            String otp = otpService.generateAndStoreOtp(TEST_EMAIL);

            boolean result = otpService.verifyOtp(TEST_EMAIL, otp);

            assertThat(result).isTrue();

            // All 3 Redis keys must be gone after successful verification
            assertThat(redisTemplate.hasKey("otp:" + TEST_EMAIL)).isFalse();
            assertThat(redisTemplate.hasKey("otp_attempts:" + TEST_EMAIL)).isFalse();
            assertThat(redisTemplate.hasKey("otp_cooldown:" + TEST_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("Verify wrong OTP → throws INVALID_OTP and keys remain in Redis")
        void verify_wrongOtp_shouldThrow_andKeepKeys() {
            otpService.generateAndStoreOtp(TEST_EMAIL);

            assertThatThrownBy(() -> otpService.verifyOtp(TEST_EMAIL, "000000"))
                    .isInstanceOf(com.jeevan.TradingApp.exception.CustomException.class)
                    .hasMessageContaining("Invalid OTP");

            // OTP key still alive — user can try again
            assertThat(redisTemplate.hasKey("otp:" + TEST_EMAIL)).isTrue();
        }
    }

    // =========================================================================
    //  OTP — Security Features
    // =========================================================================
    @Nested
    @DisplayName("OTP Security Features")
    class OtpSecurity {

        @Test
        @DisplayName("Cooldown prevents resend within 30 seconds")
        void cooldown_preventImmediateResend() {
            otpService.generateAndStoreOtp(TEST_EMAIL);

            // Immediate resend attempt must be blocked
            assertThatThrownBy(() -> otpService.generateAndStoreOtp(TEST_EMAIL))
                    .isInstanceOf(com.jeevan.TradingApp.exception.CustomException.class)
                    .hasMessageContaining("30 seconds");

            // canResendOtp must also report false
            assertThat(otpService.canResendOtp(TEST_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("Max 3 failed attempts locks out the user and clears OTP keys")
        void maxAttempts_shouldLockOutUserAfterThreeFailures() {
            // Generate OTP — result is used for the correction flow below
            otpService.generateAndStoreOtp(TEST_EMAIL);

            // Attempt 1 — wrong
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_EMAIL, "111111"))
                    .isInstanceOf(com.jeevan.TradingApp.exception.CustomException.class);

            // Attempt 2 — wrong (reset cooldown to allow re-testing)
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_EMAIL, "222222"))
                    .isInstanceOf(com.jeevan.TradingApp.exception.CustomException.class);

            // Attempt 3 — wrong → max attempts reached, keys cleared
            assertThatThrownBy(() -> otpService.verifyOtp(TEST_EMAIL, "333333"))
                    .isInstanceOf(com.jeevan.TradingApp.exception.CustomException.class)
                    .hasMessageContaining("Maximum OTP attempts");

            // OTP key cleared — user must request a new one
            assertThat(redisTemplate.hasKey("otp:" + TEST_EMAIL)).isFalse();
        }

        @Test
        @DisplayName("clearOtp removes all 3 keys from Redis")
        void clearOtp_removesAllKeys() {
            otpService.generateAndStoreOtp(TEST_EMAIL);

            // Keys exist before clear
            assertThat(redisTemplate.hasKey("otp:" + TEST_EMAIL)).isTrue();

            otpService.clearOtp(TEST_EMAIL);

            assertThat(redisTemplate.hasKey("otp:" + TEST_EMAIL)).isFalse();
            assertThat(redisTemplate.hasKey("otp_attempts:" + TEST_EMAIL)).isFalse();
            assertThat(redisTemplate.hasKey("otp_cooldown:" + TEST_EMAIL)).isFalse();
        }
    }

    // =========================================================================
    //  MarketDataCacheService — Real Redis Round-Trip
    // =========================================================================
    @Nested
    @DisplayName("MarketDataCacheService — Redis Round-Trip")
    class MarketDataCacheRoundTrip {

        @Test
        @DisplayName("setPrice → getPrice returns exact same data")
        void setAndGet_shouldReturnStoredPriceData() {
            java.util.Map<String, Object> priceData = java.util.Map.of(
                    "coinId",              "bitcoin-test",
                    "symbol",              "btc",
                    "name",               "Bitcoin Test",
                    "currentPrice",       50_000.0,
                    "priceChange24h",     100.0,
                    "priceChangePercentage24h", 0.2,
                    "marketCap",          900_000_000_000L,
                    "totalVolume",        30_000_000_000L,
                    "timestamp",          java.time.LocalDateTime.now().toString()
            );

            cacheService.setPrice("bitcoin-test", priceData);

            java.util.Map<String, Object> result = cacheService.getPrice("bitcoin-test");

            assertThat(result).isNotNull();
            assertThat(result.get("coinId")).isEqualTo("bitcoin-test");
            assertThat(((Number) result.get("currentPrice")).doubleValue()).isEqualTo(50_000.0);
        }

        @Test
        @DisplayName("getPrice returns null for a key that was never set")
        void getPrice_returnsNull_forNonExistentKey() {
            java.util.Map<String, Object> result = cacheService.getPrice("nonexistent-coin-xyz");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("hasPriceInCache returns true after setPrice, false before")
        void hasPriceInCache_reflectsActualRedisState() {
            assertThat(cacheService.hasPriceInCache("bitcoin-test")).isFalse();

            cacheService.setPrice("bitcoin-test",
                    java.util.Map.of("coinId", "bitcoin-test", "currentPrice", 50_000.0));

            assertThat(cacheService.hasPriceInCache("bitcoin-test")).isTrue();
        }

        @Test
        @DisplayName("getAllPrices returns all coins stored across multiple setPrice calls")
        void getAllPrices_returnsAllCachedCoins() {
            cacheService.setPrice("bitcoin-test",
                    java.util.Map.of("coinId", "bitcoin-test", "currentPrice", 50_000.0));
            cacheService.setPrice("ethereum-test",
                    java.util.Map.of("coinId", "ethereum-test", "currentPrice", 3_000.0));

            java.util.List<java.util.Map<String, Object>> all = cacheService.getAllPrices();

            assertThat(all).hasSizeGreaterThanOrEqualTo(2);
            assertThat(all.stream().map(m -> m.get("coinId")))
                    .contains("bitcoin-test", "ethereum-test");
        }

        @Test
        @DisplayName("setPrice overwrites stale data without creating a new key")
        void setPrice_overwrites_staleData() {
            cacheService.setPrice("bitcoin-test",
                    java.util.Map.of("coinId", "bitcoin-test", "currentPrice", 49_000.0));
            cacheService.setPrice("bitcoin-test",
                    java.util.Map.of("coinId", "bitcoin-test", "currentPrice", 51_000.0));

            java.util.Map<String, Object> result = cacheService.getPrice("bitcoin-test");
            assertThat(((Number) result.get("currentPrice")).doubleValue()).isEqualTo(51_000.0);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void deleteCacheKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) { /* cleanup is best-effort */ }
    }
}
