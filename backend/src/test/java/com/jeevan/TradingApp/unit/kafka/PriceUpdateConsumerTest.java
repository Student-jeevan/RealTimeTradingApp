package com.jeevan.TradingApp.unit.kafka;

import com.jeevan.TradingApp.domain.AlertCondition;
import com.jeevan.TradingApp.kafka.consumer.PriceUpdateConsumer;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.repository.PriceAlertRepository;
import com.jeevan.TradingApp.service.MarketDataCacheService;
import com.jeevan.TradingApp.service.PriceAlertService;
import com.jeevan.TradingApp.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PriceUpdateConsumer.
 *
 * Key behaviors:
 *  1. Updates Redis hot cache with fresh price data
 *  2. Broadcasts to per-coin and global WebSocket topics
 *  3. Checks and triggers matching price alerts (ABOVE / BELOW)
 *  4. Does NOT propagate exceptions (price data is best-effort)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PriceUpdateConsumer Unit Tests")
@SuppressWarnings("null") // Mockito matchers return unannotated types; Eclipse false-positive @NonNull violations
class PriceUpdateConsumerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private MarketDataCacheService cacheService;
    @Mock private PriceAlertRepository priceAlertRepository;
    @Mock private PriceAlertService priceAlertService;

    @InjectMocks
    private PriceUpdateConsumer consumer;

    private PriceUpdateEvent bitcoinUpdate;

    @BeforeEach
    void setUp() {
        bitcoinUpdate = TestDataBuilder.buildPriceUpdateEvent("bitcoin", 50_000.0);
    }

    // =========================================================================
    //  Redis Cache Update
    // =========================================================================
    @Nested
    @DisplayName("Redis Cache Update")
    class RedisCacheUpdate {

        @Test
        @DisplayName("Calls cacheService.setPrice with correct coinId and price data map")
        void shouldUpdateRedisCache_withCorrectCoinIdAndData() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            ArgumentCaptor<String> coinIdCaptor = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

            consumer.consume(bitcoinUpdate);

            verify(cacheService).setPrice(coinIdCaptor.capture(), dataCaptor.capture());

            assertThat(coinIdCaptor.getValue()).isEqualTo("bitcoin");
            Map<String, Object> cached = dataCaptor.getValue();
            assertThat(cached).containsKey("coinId")
                              .containsKey("currentPrice")
                              .containsKey("symbol")
                              .containsKey("timestamp");
            assertThat(cached.get("currentPrice")).isEqualTo(50_000.0);
        }

        @Test
        @DisplayName("Cache is updated before WebSocket broadcast (correct ordering)")
        void shouldUpdateCacheThenBroadcast() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            var inOrder = inOrder(cacheService, messagingTemplate);

            consumer.consume(bitcoinUpdate);

            inOrder.verify(cacheService).setPrice(anyString(), any());
            inOrder.verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), (Object) any());
        }
    }

    // =========================================================================
    //  WebSocket Broadcasting
    // =========================================================================
    @Nested
    @DisplayName("WebSocket Broadcasting")
    class WebSocketBroadcasting {

        @Test
        @DisplayName("Sends to per-coin topic /topic/prices/{coinId}")
        void shouldSendToPerCoinTopic() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            consumer.consume(bitcoinUpdate);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/prices/bitcoin"), (Object) any(Map.class));
        }

        @Test
        @DisplayName("Sends to global /topic/prices topic for dashboard views")
        void shouldSendToGlobalPricesTopic() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            consumer.consume(bitcoinUpdate);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/prices"), (Object) any(Map.class));
        }

        @Test
        @DisplayName("Sends exactly 2 WebSocket messages per price update")
        void shouldSendExactlyTwoWebSocketMessages() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            consumer.consume(bitcoinUpdate);

            // Per-coin + global = 2 broadcasts
            verify(messagingTemplate, times(2)).convertAndSend(anyString(), (Object) any(Map.class));
        }

        @Test
        @DisplayName("WebSocket payload contains all expected market data fields")
        void shouldBroadcastCompleteMarketDataPayload() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

            consumer.consume(bitcoinUpdate);

            // Capture the per-coin message
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/prices/bitcoin"), (Object) payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload)
                    .containsKey("coinId")
                    .containsKey("currentPrice")
                    .containsKey("priceChange24h")
                    .containsKey("priceChangePercentage24h")
                    .containsKey("marketCap")
                    .containsKey("totalVolume")
                    .containsKey("timestamp");
        }
    }

    // =========================================================================
    //  Price Alert Checking
    // =========================================================================
    @Nested
    @DisplayName("Price Alert Checking")
    class PriceAlertChecking {

        @Test
        @DisplayName("Triggers ABOVE alert when current price >= target price")
        void shouldTriggerAboveAlert_whenPriceCrossesTarget() {
            // Arrange — target is $49,000; current is $50,000 → alert should fire
            PriceAlert alert = buildAlert(AlertCondition.ABOVE, BigDecimal.valueOf(49_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert));

            consumer.consume(bitcoinUpdate);  // price = 50,000

            verify(priceAlertService).triggerAlert(eq(alert), any(BigDecimal.class));
        }

        @Test
        @DisplayName("Does NOT trigger ABOVE alert when current price < target price")
        void shouldNotTriggerAboveAlert_whenPriceBelowTarget() {
            // Arrange — target is $60,000; current is $50,000 → alert should NOT fire
            PriceAlert alert = buildAlert(AlertCondition.ABOVE, BigDecimal.valueOf(60_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert));

            consumer.consume(bitcoinUpdate);

            verify(priceAlertService, never()).triggerAlert(any(), any());
        }

        @Test
        @DisplayName("Triggers BELOW alert when current price <= target price")
        void shouldTriggerBelowAlert_whenPriceFallsBelowTarget() {
            // Arrange — target is $51,000; current is $50,000 → BELOW alert fires
            PriceUpdateEvent lowPriceEvent = TestDataBuilder.buildPriceUpdateEvent("bitcoin", 50_000.0);
            PriceAlert alert = buildAlert(AlertCondition.BELOW, BigDecimal.valueOf(51_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert));

            consumer.consume(lowPriceEvent);

            verify(priceAlertService).triggerAlert(eq(alert), any(BigDecimal.class));
        }

        @Test
        @DisplayName("Does NOT trigger BELOW alert when current price > target")
        void shouldNotTriggerBelowAlert_whenCurrentPriceAboveTarget() {
            PriceAlert alert = buildAlert(AlertCondition.BELOW, BigDecimal.valueOf(40_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert));

            consumer.consume(bitcoinUpdate);  // price = 50,000 > target 40,000

            verify(priceAlertService, never()).triggerAlert(any(), any());
        }

        @Test
        @DisplayName("Skips alert check when no active alerts exist for the coin")
        void shouldSkipAlertCheck_whenNoActiveAlerts() {
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin")).thenReturn(List.of());
            // Default setUp already returns empty list
            consumer.consume(bitcoinUpdate);

            verify(priceAlertService, never()).triggerAlert(any(), any());
        }

        @Test
        @DisplayName("Evaluates all active alerts for a coin, not just the first")
        void shouldCheckAllActiveAlerts_notJustFirst() {
            PriceAlert alert1 = buildAlert(AlertCondition.ABOVE, BigDecimal.valueOf(40_000));
            PriceAlert alert2 = buildAlert(AlertCondition.ABOVE, BigDecimal.valueOf(45_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert1, alert2));

            consumer.consume(bitcoinUpdate);  // price = 50,000 > both targets

            // Both alerts triggered
            verify(priceAlertService, times(2)).triggerAlert(any(), any());
        }

        @Test
        @DisplayName("Passes BigDecimal currentPrice (not double) to triggerAlert")
        void shouldPassBigDecimalPrice_toTriggerAlert() {
            PriceAlert alert = buildAlert(AlertCondition.ABOVE, BigDecimal.valueOf(49_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert));
            ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);

            consumer.consume(bitcoinUpdate);

            verify(priceAlertService).triggerAlert(any(), priceCaptor.capture());
            assertThat(priceCaptor.getValue()).isEqualByComparingTo("50000");
        }
    }

    // =========================================================================
    //  Exception Safety (best-effort delivery)
    // =========================================================================
    @Nested
    @DisplayName("Exception Safety")
    class ExceptionSafety {

        @Test
        @DisplayName("Exception in alert check does NOT propagate (price update is non-critical)")
        void shouldSwallowException_whenAlertCheckFails() {
            PriceAlert alert = buildAlert(AlertCondition.ABOVE, BigDecimal.valueOf(49_000));
            when(priceAlertRepository.findByCoinAndTriggeredFalse("bitcoin"))
                    .thenReturn(List.of(alert));
            doThrow(new RuntimeException("DB timeout"))
                    .when(priceAlertService).triggerAlert(any(), any());

            // Should NOT propagate — price updates are best-effort
            assertThatCode(() -> consumer.consume(bitcoinUpdate)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Exception in Redis cache update does NOT block WebSocket broadcast")
        void shouldHandleCacheFailureGracefully() {
            doThrow(new RuntimeException("Redis connection refused"))
                    .when(cacheService).setPrice(anyString(), any());

            // The outer try-catch in consume() should absorb this
            assertThatCode(() -> consumer.consume(bitcoinUpdate)).doesNotThrowAnyException();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PriceAlert buildAlert(AlertCondition condition, BigDecimal targetPrice) {
        PriceAlert alert = new PriceAlert();
        alert.setId(1L);
        alert.setCoin("bitcoin");
        alert.setAlertCondition(condition);
        alert.setTargetPrice(targetPrice);
        alert.setTriggered(false);
        return alert;
    }
}
