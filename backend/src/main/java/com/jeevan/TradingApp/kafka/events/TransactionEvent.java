package com.jeevan.TradingApp.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published on every wallet transaction (credit, debit, lock, release, fee).
 * The TransactionConsumer stores immutable audit logs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Long userId;
    private String transactionType;   // CREDIT, DEBIT, TRADE_LOCK, TRADE_RELEASE, FEE
    private BigDecimal amount;
    private String referenceId;
    private String description;
}
