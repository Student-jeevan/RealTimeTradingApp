package com.jeevan.TradingApp.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published when a user needs to be notified (trade execution, price alert, etc.).
 * The NotificationConsumer sends email and WebSocket push.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Long userId;
    private String email;
    private String type;    // TRADE, ALERT, WITHDRAWAL
    private String subject;
    private String body;    // HTML body for email
}
