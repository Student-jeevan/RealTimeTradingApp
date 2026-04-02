package com.jeevan.TradingApp.unit.kafka;

import com.jeevan.TradingApp.kafka.consumer.NotificationConsumer;
import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.modal.ProcessedEvent;
import com.jeevan.TradingApp.repository.ProcessedEventRepository;
import com.jeevan.TradingApp.testutil.TestDataBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationConsumer.
 *
 * Key behaviors:
 *  1. Idempotency — duplicate events skipped (no email, no WebSocket)
 *  2. Email delivery via JavaMailSender (HTML mime message)
 *  3. WebSocket push via convertAndSendToUser to user's notification queue
 *  4. ProcessedEvent persisted after successful delivery
 *  5. Mail failure triggers re-throw → Kafka retry + DLT routing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer Unit Tests")
class NotificationConsumerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private JavaMailSender javaMailSender;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private NotificationConsumer consumer;

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
        @DisplayName("Skips email and WebSocket when event already processed")
        void shouldSkipDelivery_whenEventAlreadyProcessed() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

            consumer.consume(event);

            // No email or WebSocket delivery
            verify(javaMailSender, never()).send(any(MimeMessage.class));
            verifyNoInteractions(messagingTemplate);
            verify(processedEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Processes delivery exactly once on first occurrence")
        void shouldDeliverOnlyOnce_onFirstOccurrence() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            consumer.consume(event);

            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }
    }

    // =========================================================================
    //  Email Delivery
    // =========================================================================
    @Nested
    @DisplayName("Email Delivery")
    class EmailDelivery {

        @Test
        @DisplayName("Creates MIME message and sends it via JavaMailSender")
        void shouldCreateAndSendMimeMessage() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            consumer.consume(event);

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender).send(any(MimeMessage.class));
        }
    }

    // =========================================================================
    //  WebSocket Push
    // =========================================================================
    @Nested
    @DisplayName("WebSocket Push")
    class WebSocketPush {

        @Test
        @DisplayName("Pushes to /queue/notifications for the correct user")
        void shouldPushToUserNotificationQueue_withCorrectUserId() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            event.setUserId(42L);
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            consumer.consume(event);

            verify(messagingTemplate).convertAndSendToUser(
                    eq("42"),              // userId as String
                    eq("/queue/notifications"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("WebSocket payload contains type, subject, and timestamp")
        void shouldPushPayload_withTypeSubjectAndTimestamp() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("ALERT");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

            consumer.consume(event);

            verify(messagingTemplate).convertAndSendToUser(
                    anyString(), anyString(), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsKey("type")
                               .containsKey("subject")
                               .containsKey("timestamp");
            assertThat(payload.get("type")).isEqualTo("ALERT");
        }
    }

    // =========================================================================
    //  ProcessedEvent persistence
    // =========================================================================
    @Nested
    @DisplayName("ProcessedEvent Persistence")
    class ProcessedEventPersistence {

        @Test
        @DisplayName("Saves ProcessedEvent with the notification eventId after delivery")
        void shouldSaveProcessedEvent_afterSuccessfulDelivery() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);

            consumer.consume(event);

            verify(processedEventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventId()).isEqualTo(event.getEventId());
        }

        @Test
        @DisplayName("ProcessedEvent NOT saved when mail send fails (forces retry)")
        void shouldNotSaveProcessedEvent_whenMailSendFails() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("SMTP refused")).when(javaMailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> consumer.consume(event))
                    .isInstanceOf(RuntimeException.class);

            // Not saved → retry will attempt delivery again
            verify(processedEventRepository, never()).save(any());
        }
    }

    // =========================================================================
    //  Error handling (DLT routing)
    // =========================================================================
    @Nested
    @DisplayName("Error Handling & DLT")
    class ErrorHandling {

        @Test
        @DisplayName("Re-throws RuntimeException when email delivery fails (triggers DLT)")
        void shouldRethrow_whenMailDeliveryFails() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("WITHDRAWAL_APPROVED");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("Mail server down"))
                    .when(javaMailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> consumer.consume(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Mail server down");
        }

        @Test
        @DisplayName("Re-throws when MessagingException during MIME construction")
        void shouldRethrow_whenMessagingExceptionOccurs() throws MessagingException {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            // Simulate MIME construction failure — MimeMessageHelper will throw
            when(javaMailSender.createMimeMessage())
                    .thenThrow(new RuntimeException(new MessagingException("Bad address")));

            assertThatThrownBy(() -> consumer.consume(event))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Email and WebSocket delivery happens BEFORE marking as processed")
        void shouldDeliverBeforeMarkingProcessed() {
            NotificationEvent event = TestDataBuilder.buildNotificationEvent("TRADE");
            when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            var inOrder = inOrder(javaMailSender, messagingTemplate, processedEventRepository);

            consumer.consume(event);

            inOrder.verify(javaMailSender).send(any(MimeMessage.class));
            inOrder.verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
            inOrder.verify(processedEventRepository).save(any());
        }
    }
}
