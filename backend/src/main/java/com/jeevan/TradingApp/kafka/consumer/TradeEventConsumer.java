package com.jeevan.TradingApp.kafka.consumer;

import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.kafka.producer.NotificationEventProducer;
import com.jeevan.TradingApp.kafka.producer.TransactionEventProducer;
import com.jeevan.TradingApp.modal.ProcessedEvent;
import com.jeevan.TradingApp.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumes trade events after a BUY/SELL is committed.
 * Side effects: publishes notification and transaction audit events.
 * Idempotent — skips events already processed.
 */
@Service
public class TradeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private NotificationEventProducer notificationProducer;

    @Autowired
    private TransactionEventProducer transactionProducer;

    @KafkaListener(topics = "trade-events", groupId = "trade-group")
    public void consume(TradeEvent event) {
        log.info("[TradeEventConsumer] Received: eventId={}, orderId={}, type={}",
                event.getEventId(), event.getOrderId(), event.getOrderType());

        // --- Idempotency check ---
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("[TradeEventConsumer] Duplicate event {}, skipping.", event.getEventId());
            return;
        }

        try {
            // 1. Publish notification event for the user
            NotificationEvent notification = NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .userId(event.getUserId())
                    .email(event.getUserEmail())
                    .type("TRADE")
                    .subject("Trade Executed: " + event.getOrderType() + " " + event.getCoinSymbol().toUpperCase())
                    .body(buildTradeEmailBody(event))
                    .build();
            notificationProducer.publish(notification);

            // 2. Publish transaction audit event
            TransactionEvent txnEvent = TransactionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .userId(event.getUserId())
                    .transactionType(event.getOrderType().equals("BUY") ? "DEBIT" : "CREDIT")
                    .amount(event.getPrice())
                    .referenceId(String.valueOf(event.getOrderId()))
                    .description("Trade " + event.getOrderType() + " " + event.getQuantity() + " " + event.getCoinSymbol())
                    .build();
            transactionProducer.publish(txnEvent);

            // 3. Mark as processed
            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(event.getEventId());
            processedEventRepository.save(pe);

            log.info("[TradeEventConsumer] Successfully processed event {}", event.getEventId());
        } catch (Exception e) {
            log.error("[TradeEventConsumer] Error processing event {}: {}", event.getEventId(), e.getMessage(), e);
            throw e; // let error handler retry
        }
    }

    private String buildTradeEmailBody(TradeEvent event) {
        String action = event.getOrderType().equals("BUY") ? "purchased" : "sold";
        String color = event.getOrderType().equals("BUY") ? "#16a34a" : "#dc2626";
        return """
                <div style="font-family: Helvetica, Arial, sans-serif; background: #f9f9f9; padding: 40px 20px;">
                  <div style="max-width: 500px; margin: 0 auto; background: #fff; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);">
                    <h2 style="color: #1a1a1a;">Trade Executed ✅</h2>
                    <p>You have successfully <strong style="color:%s">%s</strong> <strong>%s %s</strong> at <strong>$%s</strong>.</p>
                    <p style="color: #777; font-size: 13px;">Order ID: %s</p>
                  </div>
                </div>
                """.formatted(color, action, event.getQuantity(), event.getCoinSymbol().toUpperCase(),
                event.getPrice().toPlainString(), event.getOrderId());
    }
}
