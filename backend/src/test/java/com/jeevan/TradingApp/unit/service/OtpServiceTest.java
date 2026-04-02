package com.jeevan.TradingApp.unit.service;

import com.jeevan.TradingApp.exception.CustomException;
import com.jeevan.TradingApp.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OtpService.
 *
 * Strategy:
 *  - Mock RedisTemplate + ValueOperations entirely.
 *  - Test every branch: cooldown, max-attempts, expired, wrong OTP, success.
 *  - Never touch a real Redis instance — fast and isolated.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Unit Tests")
class OtpServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    // ── System Under Test ────────────────────────────────────────────────────
    @InjectMocks
    private OtpService otpService;

    private static final String EMAIL = "user@trading.com";
    private static final String OTP_KEY       = "otp:" + EMAIL;
    private static final String ATTEMPTS_KEY  = "otp_attempts:" + EMAIL;
    private static final String COOLDOWN_KEY  = "otp_cooldown:" + EMAIL;

    @BeforeEach
    void setUp() {
        // No global stubs — each test mocks only what it needs
    }

    // =========================================================================
    //  generateAndStoreOtp
    // =========================================================================
    @Nested
    @DisplayName("generateAndStoreOtp")
    class GenerateAndStoreOtp {

        @Test
        @DisplayName("Happy path: generates 6-digit OTP, stores 3 keys, returns OTP string")
        void shouldGenerateAndStoreOtp_whenNoCooldownExists() {
            // Arrange — no cooldown active
            when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            // Act
            String otp = otpService.generateAndStoreOtp(EMAIL);

            // Assert — OTP must be a 6-digit numeric string
            assertThat(otp).isNotNull()
                    .as("OTP must be exactly 6 digits")
                    .matches("\\d{6}");

            // Assert — OTP value stored with 5-min TTL
            verify(valueOps).set(eq(OTP_KEY), eq((Object) otp), eq(5L), eq(TimeUnit.MINUTES));
            // Assert — attempts reset to 0
            verify(valueOps).set(eq(ATTEMPTS_KEY), eq((Object) Integer.valueOf(0)), eq(5L), eq(TimeUnit.MINUTES));
            // Assert — cooldown key set for 30 seconds
            verify(valueOps).set(eq(COOLDOWN_KEY), any(), eq(30L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Throws OTP_COOLDOWN when cooldown key is still active in Redis")
        void shouldThrowOtpCooldown_whenCooldownKeyExists() {
            // Arrange — cooldown is still active
            when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> otpService.generateAndStoreOtp(EMAIL))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("30 seconds");

            // Verify that NO Redis writes occurred (fail-fast)
            verify(valueOps, never()).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("Each call generates a different OTP (randomness check)")
        void shouldGenerateDifferentOtpsOnMultipleCalls() {
            when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(valueOps);

            String otp1 = otpService.generateAndStoreOtp(EMAIL);

            // Reset cooldown mock for second call
            when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(false);
            String otp2 = otpService.generateAndStoreOtp(EMAIL);

            // They COULD theoretically match, but the distribution makes this astronomically unlikely
            assertThat(otp1).isNotEqualTo(otp2);
        }
    }

    // =========================================================================
    //  verifyOtp
    // =========================================================================
    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtp {

        @Test
        @DisplayName("Happy path: correct OTP on first attempt returns true and clears all keys")
        void shouldReturnTrue_whenOtpIsCorrectOnFirstAttempt() {
            // Arrange
            String correctOtp = "123456";
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(ATTEMPTS_KEY)).thenReturn("0");
            when(valueOps.get(OTP_KEY)).thenReturn(correctOtp);

            // Act
            boolean result = otpService.verifyOtp(EMAIL, correctOtp);

            // Assert
            assertThat(result).isTrue();
            // All 3 Redis keys must be deleted on success
            verify(redisTemplate).delete(OTP_KEY);
            verify(redisTemplate).delete(ATTEMPTS_KEY);
            verify(redisTemplate).delete(COOLDOWN_KEY);
        }

        @Test
        @DisplayName("Throws INVALID_OTP and increments counter when OTP is wrong")
        void shouldThrowInvalidOtp_whenOtpDoesNotMatch() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(ATTEMPTS_KEY)).thenReturn("0");
            when(valueOps.get(OTP_KEY)).thenReturn("654321");

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "999999"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Invalid OTP");

            // Attempt counter must be incremented
            verify(valueOps).increment(ATTEMPTS_KEY);
        }

        @Test
        @DisplayName("Throws OTP_EXPIRED when OTP key is no longer in Redis")
        void shouldThrowOtpExpired_whenOtpKeyMissingFromRedis() {
            // Arrange — OTP key TTL expired
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(ATTEMPTS_KEY)).thenReturn("0");
            when(valueOps.get(OTP_KEY)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "123456"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Throws OTP_MAX_ATTEMPTS and clears keys after 3 failed attempts")
        void shouldThrowMaxAttempts_andClearKeys_afterThreeFailures() {
            // Arrange — already at 3 attempts
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(ATTEMPTS_KEY)).thenReturn("3");

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "000000"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Maximum OTP attempts");

            // Keys must be cleaned up to force user to request new OTP
            verify(redisTemplate).delete(OTP_KEY);
            verify(redisTemplate).delete(ATTEMPTS_KEY);
            verify(redisTemplate).delete(COOLDOWN_KEY);
        }

        @Test
        @DisplayName("Shows remaining attempts count in error message")
        void shouldIncludeRemainingAttemptsInMessage() {
            // Arrange — 1 attempt already used
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(ATTEMPTS_KEY)).thenReturn("1");
            when(valueOps.get(OTP_KEY)).thenReturn("654321");

            // Act & Assert
            assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "999999"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("1 attempt(s) remaining");
        }

        @Test
        @DisplayName("Handles null attempts value (first request) gracefully")
        void shouldDefaultToZeroAttempts_whenAttemptsKeyIsNull() {
            // Arrange — attempts key does not exist yet
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(ATTEMPTS_KEY)).thenReturn(null);
            when(valueOps.get(OTP_KEY)).thenReturn("123456");

            // Act
            boolean result = otpService.verifyOtp(EMAIL, "123456");

            // Assert — null attempts treated as 0, verification succeeds
            assertThat(result).isTrue();
        }
    }

    // =========================================================================
    //  canResendOtp
    // =========================================================================
    @Nested
    @DisplayName("canResendOtp")
    class CanResendOtp {

        @Test
        @DisplayName("Returns true when cooldown has expired (key absent)")
        void shouldReturnTrue_whenCooldownExpired() {
            when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(false);
            assertThat(otpService.canResendOtp(EMAIL)).isTrue();
        }

        @Test
        @DisplayName("Returns false when cooldown is still active (key present)")
        void shouldReturnFalse_whenCooldownActive() {
            when(redisTemplate.hasKey(COOLDOWN_KEY)).thenReturn(true);
            assertThat(otpService.canResendOtp(EMAIL)).isFalse();
        }
    }

    // =========================================================================
    //  clearOtp
    // =========================================================================
    @Nested
    @DisplayName("clearOtp")
    class ClearOtp {

        @Test
        @DisplayName("Deletes all 3 Redis keys atomically")
        void shouldDeleteAllThreeKeys() {
            // Act
            otpService.clearOtp(EMAIL);

            // Assert — all keys deleted
            verify(redisTemplate).delete(OTP_KEY);
            verify(redisTemplate).delete(ATTEMPTS_KEY);
            verify(redisTemplate).delete(COOLDOWN_KEY);
            verifyNoMoreInteractions(redisTemplate);
        }
    }
}
