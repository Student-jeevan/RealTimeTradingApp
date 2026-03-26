package com.jeevan.TradingApp.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Separate idempotency table for analytics consumers.
 * Prevents analytics from coupling with the existing ProcessedEvent table.
 */
@Entity
@Table(name = "analytics_processed_events",
       indexes = @Index(name = "idx_analytics_event_id", columnList = "eventId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String consumerGroup;

    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        this.processedAt = LocalDateTime.now();
    }
}
