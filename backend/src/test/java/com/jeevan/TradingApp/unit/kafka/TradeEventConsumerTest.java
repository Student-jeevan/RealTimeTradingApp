package com.jeevan.TradingApp.unit.kafka;

import com.jeevan.TradingApp.kafka.consumer.TradeEventConsumer;
import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.kafka.producer.NotificationEventProducer;
import com.jeevan.TradingApp.kafka.producer.TransactionEventProducer;
import com.jeevan.TradingApp.modal.ProcessedEvent;
import com.jeevan.TradingApp.repository.ProcessedEventRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradeEventConsumer.
 *
 * Key behaviors to verify:
 *  1. Idempotency: duplicate eventIds are skipped silently
 *  2. Fan-out: produces NotificationEvent + TransactionEvent on first processing
 *  3. Audit: ProcessedEvent saved after successful handling
 *  4. Error propagation: exceptions re-thrown to trigger Kafka retry/DLT
 *  5. BUY/SELL distinction in TransactionEvent type (DEBIT/CREDIT)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeEventConsumer Unit Tests")
class TradeEventConsumerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private NotificationEventProducer notificationProducer;
    @Mock private TransactionEventProducer transactionProducer;

    @InjectMocks
    private TradeEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // No global stubs — each test mocks only what it needs
    }

    // =========================================================================
    //  Idempotency
    // =========================================================================
    @Nested
    @DisplayName("Idempotency Guard")
    class IdempotencyGuard {

        @Test
        @DisplayName("Skips all processing when eventId is already in processed_events")
        void shouldSkipEvent_whenAlreadyProcessed() {
            // Arrange — eventId already recorded
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

            // Act
            consumer.consume(event);

            // Assert — no side-effect producers invoked
            verifyNoInteractions(notificationProducer);
            verifyNoInteractions(transactionProducer);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Processes event normally on first occurrence")
        void shouldProcessEvent_whenNotSeenBefore() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.consume(event);

            // Both fan-out events published
            verify(notificationProducer).publish(any(NotificationEvent.class));
            verify(transactionProducer).publish(any(TransactionEvent.class));
            // Marked as processed
            verify(processedEventRepository).save(any(ProcessedEvent.class));
        }
    }

    // =========================================================================
    //  Fan-out: Notification publishing
    // =========================================================================
    @Nested
    @DisplayName("Notification Event Publishing")
    class NotificationEventPublishing {

        @Test
        @DisplayName("Publishes NotificationEvent with TRADE type and correct user context")
        void shouldPublishTradeNotification_withCorrectFields() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);

            consumer.consume(event);

            verify(notificationProducer).publish(captor.capture());
            NotificationEvent notification = captor.getValue();

            assertThat(notification.getType()).isEqualTo("TRADE");
            assertThat(notification.getUserId()).isEqualTo(event.getUserId());
            assertThat(notification.getEmail()).isEqualTo(event.getUserEmail());
            assertThat(notification.getSubject()).contains("BUY");
            assertThat(notification.getEventId()).isNotBlank();
            assertThat(notification.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("SELL trade notification subject contains SELL keyword")
        void shouldIncludeSell_inSubject_forSellTrade() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("SELL");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);

            consumer.consume(event);

            verify(notificationProducer).publish(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("SELL");
        }

        @Test
        @DisplayName("Notification HTML body contains coin symbol in uppercase")
        void shouldContainCoinSymbol_inEmailBody() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            event.setCoinSymbol("btc");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);

            consumer.consume(event);

            verify(notificationProducer).publish(captor.capture());
            assertThat(captor.getValue().getBody()).contains("BTC");
        }

        @Test
        @DisplayName("Each notification has a unique UUID eventId (not same as trade eventId)")
        void shouldAssignNewEventId_toNotification() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);

            consumer.consume(event);

            verify(notificationProducer).publish(captor.capture());
            // Notification gets its own fresh UUID
            assertThat(captor.getValue().getEventId()).isNotEqualTo(event.getEventId());
        }
    }

    // =========================================================================
    //  Fan-out: Transaction audit publishing
    // =========================================================================
    @Nested
    @DisplayName("Transaction Audit Event Publishing")
    class TransactionAuditPublishing {

        @Test
        @DisplayName("BUY trade produces TransactionEvent with DEBIT type")
        void shouldPublishDebitAudit_forBuyTrade() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);

            consumer.consume(event);

            verify(transactionProducer).publish(captor.capture());
            TransactionEvent txn = captor.getValue();

            assertThat(txn.getTransactionType()).isEqualTo("DEBIT");
            assertThat(txn.getUserId()).isEqualTo(event.getUserId());
            assertThat(txn.getAmount()).isEqualByComparingTo(event.getPrice());
            assertThat(txn.getReferenceId()).isEqualTo(String.valueOf(event.getOrderId()));
        }

        @Test
        @DisplayName("SELL trade produces TransactionEvent with CREDIT type")
        void shouldPublishCreditAudit_forSellTrade() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("SELL");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);

            consumer.consume(event);

            verify(transactionProducer).publish(captor.capture());
            assertThat(captor.getValue().getTransactionType()).isEqualTo("CREDIT");
        }

        @Test
        @DisplayName("TransactionEvent description includes coin symbol and quantity")
        void shouldIncludeCoinAndQuantity_inDescription() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            event.setCoinSymbol("eth");
            event.setQuantity(2.5);
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);

            consumer.consume(event);

            verify(transactionProducer).publish(captor.capture());
            assertThat(captor.getValue().getDescription())
                    .containsIgnoringCase("eth")
                    .contains("2.5");
        }
    }

    // =========================================================================
    //  ProcessedEvent persistence
    // =========================================================================
    @Nested
    @DisplayName("ProcessedEvent Persistence")
    class ProcessedEventPersistence {

        @Test
        @DisplayName("Saves ProcessedEvent with the exact trade eventId after successful processing")
        void shouldSaveProcessedEvent_withCorrectEventId() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);

            consumer.consume(event);

            verify(processedEventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventId()).isEqualTo(event.getEventId());
        }
    }

    // =========================================================================
    //  Error handling & retry
    // =========================================================================
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Re-throws exception when notification producer fails (triggers Kafka retry)")
        void shouldRethrowException_whenNotificationProducerFails() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            doThrow(new RuntimeException("Kafka broker unavailable"))
                    .when(notificationProducer).publish(any());

            assertThatThrownBy(() -> consumer.consume(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Kafka broker unavailable");

            // ProcessedEvent must NOT be saved — so retry will reprocess the event
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Re-throws exception when transaction producer fails")
        void shouldRethrowException_whenTransactionProducerFails() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("SELL");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            doThrow(new RuntimeException("Serialization error"))
                    .when(transactionProducer).publish(any());

            assertThatThrownBy(() -> consumer.consume(event))
                    .isInstanceOf(RuntimeException.class);

            verify(processedEventRepository, never()).save(any());
        }
    }
}
