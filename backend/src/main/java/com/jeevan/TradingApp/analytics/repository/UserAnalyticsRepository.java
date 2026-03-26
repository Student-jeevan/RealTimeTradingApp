package com.jeevan.TradingApp.analytics.repository;

import com.jeevan.TradingApp.analytics.model.UserAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAnalyticsRepository extends JpaRepository<UserAnalytics, Long> {
    Optional<UserAnalytics> findByUserId(Long userId);
}
