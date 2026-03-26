package com.jeevan.TradingApp.analytics.repository;

import com.jeevan.TradingApp.analytics.model.AnalyticsProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsProcessedEventRepository extends JpaRepository<AnalyticsProcessedEvent, Long> {
    boolean existsByEventId(String eventId);
}
