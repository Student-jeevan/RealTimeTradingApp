package com.jeevan.TradingApp.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published after a trade (BUY/SELL) is successfully executed.
 * Consumers use this for notifications and audit logging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Long userId;
    private String userEmail;
    private Long orderId;
    private String orderType;   // BUY or SELL
    private String coinId;
    private String coinSymbol;
    private double quantity;
    private BigDecimal price;
    private String status;      // FILLED, CANCELLED, etc.
}
