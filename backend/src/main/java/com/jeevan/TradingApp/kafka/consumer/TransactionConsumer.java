package com.jeevan.TradingApp.kafka.consumer;

import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.modal.ProcessedEvent;
import com.jeevan.TradingApp.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes transaction events and stores immutable audit log entries.
 * Uses a dedicated audit_log table separate from the wallet_ledger.
 * Idempotent — duplicate events are skipped.
 */
@Service
public class TransactionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionConsumer.class);

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "transaction-events", groupId = "txn-audit-group")
    public void consume(TransactionEvent event) {
        log.info("[TransactionConsumer] Received: type={}, userId={}, eventId={}",
                event.getTransactionType(), event.getUserId(), event.getEventId());

        // --- Idempotency check ---
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.warn("[TransactionConsumer] Duplicate event {}, skipping.", event.getEventId());
            return;
        }

        try {
            // Insert immutable audit log entry
            jdbcTemplate.update(
                    """
                    INSERT INTO audit_log (event_id, user_id, transaction_type, amount, reference_id, description, event_timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    event.getEventId(),
                    event.getUserId(),
                    event.getTransactionType(),
                    event.getAmount(),
                    event.getReferenceId(),
                    event.getDescription(),
                    event.getTimestamp()
            );

            // Mark as processed
            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(event.getEventId());
            processedEventRepository.save(pe);

            log.info("[TransactionConsumer] Audit log stored for event {}", event.getEventId());
        } catch (Exception e) {
            log.error("[TransactionConsumer] Failed for event {}: {}", event.getEventId(), e.getMessage(), e);
            throw new RuntimeException(e); // trigger retry / DLT
        }
    }
}
