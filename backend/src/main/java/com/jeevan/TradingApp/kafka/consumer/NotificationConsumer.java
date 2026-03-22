package com.jeevan.TradingApp.kafka.consumer;

import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.modal.ProcessedEvent;
import com.jeevan.TradingApp.repository.ProcessedEventRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Consumes notification events and delivers via email + WebSocket.
 * Idempotent — duplicate events are skipped.
 * Failed messages retry 3x then go to notification-events.DLT.
 */
@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "notification-events", groupId = "notification-group")
    public void consume(NotificationEvent event) {
        log.info("[NotificationConsumer] Received: type={}, userId={}, eventId={}",
                event.getType(), event.getUserId(), event.getEventId());

        // --- Idempotency check ---
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("[NotificationConsumer] Duplicate event {}, skipping.", event.getEventId());
            return;
        }

        try {
            // 1. Send email
            sendEmail(event);

            // 2. Push WebSocket notification to user
            messagingTemplate.convertAndSendToUser(
                    event.getUserId().toString(),
                    "/queue/notifications",
                    Map.of(
                            "type", event.getType(),
                            "subject", event.getSubject(),
                            "timestamp", event.getTimestamp().toString()
                    )
            );

            // 3. Mark as processed
            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(event.getEventId());
            processedEventRepository.save(pe);

            log.info("[NotificationConsumer] Delivered notification {} to {}", event.getEventId(), event.getEmail());
        } catch (Exception e) {
            log.error("[NotificationConsumer] Failed for event {}: {}", event.getEventId(), e.getMessage(), e);
            throw new RuntimeException(e); // trigger retry / DLT
        }
    }

    private void sendEmail(NotificationEvent event) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
        helper.setTo(event.getEmail());
        helper.setSubject(event.getSubject());
        helper.setText(event.getBody(), true);
        javaMailSender.send(mimeMessage);
    }
}
