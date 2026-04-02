package com.jeevan.TradingApp.integration;

import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.kafka.producer.NotificationEventProducer;
import com.jeevan.TradingApp.kafka.producer.TradeEventProducer;
import com.jeevan.TradingApp.kafka.producer.TransactionEventProducer;
import com.jeevan.TradingApp.modal.ProcessedEvent;
import com.jeevan.TradingApp.repository.ProcessedEventRepository;
import com.jeevan.TradingApp.testutil.TestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the complete Kafka event pipeline:
 *
 *  TradeEvent produced → TradeEventConsumer
 *    → publishes NotificationEvent → NotificationConsumer (email + WS)
 *    → publishes TransactionEvent  → TransactionConsumer (audit_log insert)
 *    → saves ProcessedEvent (idempotency marker)
 *
 * Uses real Kafka Testcontainer and Spring Boot full context.
 * Awaitility polls asserts asynchronously (no Thread.sleep magic numbers).
 *
 * NOTE: Add Awaitility to pom.xml:
 *   <groupId>org.awaitility</groupId><artifactId>awaitility</artifactId>
 */
@DisplayName("Kafka Event Pipeline Integration Tests")
@SuppressWarnings("null") // Test uses null Session for MimeMessage construction — safe in unit context
class KafkaEventPipelineIntegrationTest extends BaseIntegrationTest {

    @Autowired private TradeEventProducer tradeEventProducer;
    @Autowired private NotificationEventProducer notificationEventProducer;
    @Autowired private TransactionEventProducer transactionEventProducer;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Mock mail sender to prevent real SMTP calls
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        // Ensure audit_log table exists (create-drop DDL may not have it — add manually)
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                    event_id      VARCHAR(100) NOT NULL UNIQUE,
                    user_id       BIGINT,
                    transaction_type VARCHAR(50),
                    amount        DECIMAL(18,8),
                    reference_id  VARCHAR(100),
                    description   VARCHAR(255),
                    event_timestamp DATETIME
                )
                """);
        processedEventRepository.deleteAll();
    }

    // =========================================================================
    //  Trade Event Full Pipeline
    // =========================================================================
    @Nested
    @DisplayName("TradeEvent → Consumer Pipeline")
    class TradeEventPipeline {

        @Test
        @DisplayName("Producing a TradeEvent results in a ProcessedEvent being saved by consumer")
        void tradeEvent_shouldBeConsumedAndMarkedProcessed() {
            // Arrange
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");

            // Act
            tradeEventProducer.publish(event);

            // Assert — wait up to 10s for async consumer to save ProcessedEvent
            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(300, TimeUnit.MILLISECONDS)
                    .untilAsserted(() ->
                            assertThat(processedEventRepository.existsByEventId(event.getEventId()))
                                    .isTrue()
                    );
        }

        @Test
        @DisplayName("Duplicate TradeEvent is NOT processed twice (idempotency)")
        void duplicateTradeEvent_shouldBeSkipped() {
            // Arrange — pre-mark event as already processed
            String sharedEventId = UUID.randomUUID().toString();
            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(sharedEventId);
            processedEventRepository.save(pe);

            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            event.setEventId(sharedEventId); // same eventId

            long countBefore = processedEventRepository.count();

            // Act
            tradeEventProducer.publish(event);

            // Wait briefly then assert count has NOT increased
            await().during(2, TimeUnit.SECONDS)
                    .atMost(3, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            assertThat(processedEventRepository.count()).isEqualTo(countBefore)
                    );
        }

        @Test
        @DisplayName("BUY TradeEvent causes DEBIT audit_log entry via TransactionConsumer")
        void buyTradeEvent_shouldInsertDebitAuditLog() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("BUY");
            event.setEventId(UUID.randomUUID().toString());

            tradeEventProducer.publish(event);

            // Wait for TransactionConsumer to insert into audit_log
            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(300, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE user_id = ?",
                                Integer.class,
                                event.getUserId()
                        );
                        assertThat(count).isGreaterThan(0);
                    });

            // Verify DEBIT type
            String txnType = jdbcTemplate.queryForObject(
                    "SELECT transaction_type FROM audit_log WHERE user_id = ? LIMIT 1",
                    String.class,
                    event.getUserId()
            );
            assertThat(txnType).isEqualTo("DEBIT");
        }

        @Test
        @DisplayName("SELL TradeEvent produces CREDIT audit_log entry")
        void sellTradeEvent_shouldInsertCreditAuditLog() {
            TradeEvent event = TestDataBuilder.buildTradeEvent("SELL");
            event.setEventId(UUID.randomUUID().toString());
            event.setUserId(2L);  // different user to avoid interference

            tradeEventProducer.publish(event);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(300, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE user_id = 2",
                                Integer.class
                        );
                        assertThat(count).isGreaterThan(0);
                    });
        }
    }

    // =========================================================================
    //  Notification Event Pipeline
    // =========================================================================
    @Nested
    @DisplayName("NotificationEvent → Consumer Pipeline")
    class NotificationEventPipeline {

        @Test
        @DisplayName("Notification event is consumed and marked as processed")
        void notificationEvent_shouldBeConsumedAndProcessed() {
            // Configure mock mail sender
            org.mockito.Mockito.doNothing().when(javaMailSender).send(
                    org.mockito.ArgumentMatchers.any(jakarta.mail.internet.MimeMessage.class));
            org.mockito.Mockito.when(javaMailSender.createMimeMessage())
                    .thenReturn(new jakarta.mail.internet.MimeMessage(
                            (jakarta.mail.Session) null));

            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");

            notificationEventProducer.publish(event);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(300, TimeUnit.MILLISECONDS)
                    .untilAsserted(() ->
                            assertThat(processedEventRepository.existsByEventId(event.getEventId()))
                                    .isTrue()
                    );
        }

        @Test
        @DisplayName("Duplicate NotificationEvent is skipped by idempotency guard")
        void duplicateNotificationEvent_isSkipped() {
            String eventId = UUID.randomUUID().toString();
            processedEventRepository.save(new ProcessedEvent(null, eventId, null));

            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            event.setEventId(eventId);

            long before = processedEventRepository.count();
            notificationEventProducer.publish(event);

            await().during(2, TimeUnit.SECONDS)
                    .atMost(3, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            assertThat(processedEventRepository.count()).isEqualTo(before)
                    );
        }
    }

    // =========================================================================
    //  Transaction Event Pipeline
    // =========================================================================
    @Nested
    @DisplayName("TransactionEvent → Consumer Pipeline")
    class TransactionEventPipeline {

        @Test
        @DisplayName("Transaction event produces audit_log row in DB")
        void transactionEvent_shouldInsertAuditLogRow() {
            TransactionEvent event = TestDataBuilder.buildTransactionEvent("CREDIT");
            event.setEventId(UUID.randomUUID().toString());
            event.setUserId(999L);

            transactionEventProducer.publish(event);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(300, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE event_id = ?",
                                Integer.class,
                                event.getEventId()
                        );
                        assertThat(count).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("Duplicate TransactionEvent is not inserted twice into audit_log")
        void duplicateTransactionEvent_isNotInsertedTwice() {
            TransactionEvent event = TestDataBuilder.buildTransactionEvent("DEBIT");
            event.setEventId(UUID.randomUUID().toString());

            // Pre-save process marker
            processedEventRepository.save(new ProcessedEvent(null, event.getEventId(), null));

            transactionEventProducer.publish(event);

            // Wait then verify only the pre-existing marker — NOT a second audit_log row
            await().during(3, TimeUnit.SECONDS)
                    .atMost(4, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM audit_log WHERE event_id = ?",
                                Integer.class,
                                event.getEventId()
                        );
                        assertThat(count).isZero(); // skipped by idempotency
                    });
        }
    }
}
