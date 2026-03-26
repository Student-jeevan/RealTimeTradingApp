package com.jeevan.TradingApp.analytics.producer;

import com.jeevan.TradingApp.kafka.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Publishes analytics-specific events to the analytics-events topic.
 *
 * Use cases:
 *   - HIGH_RISK_ALERT:       when a user's risk score exceeds threshold
 *   - PORTFOLIO_DROP_ALERT:  when portfolio drops more than X% in a day
 */
@Service
public class AnalyticsEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a high-risk alert for a user.
     */
    public void publishHighRiskAlert(Long userId, int riskScore, String riskLevel) {
        Map<String, Object> event = Map.of(
                "type", "HIGH_RISK_ALERT",
                "userId", userId,
                "riskScore", riskScore,
                "riskLevel", riskLevel,
                "timestamp", LocalDateTime.now().toString()
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_ANALYTICS_EVENTS, userId.toString(), event);
        log.info("[AnalyticsProducer] Published HIGH_RISK_ALERT for user={} score={}", userId, riskScore);
    }

    /**
     * Publish a portfolio drop alert for a user.
     */
    public void publishPortfolioDropAlert(Long userId, double dropPercentage) {
        Map<String, Object> event = Map.of(
                "type", "PORTFOLIO_DROP_ALERT",
                "userId", userId,
                "dropPercentage", dropPercentage,
                "timestamp", LocalDateTime.now().toString()
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_ANALYTICS_EVENTS, userId.toString(), event);
        log.info("[AnalyticsProducer] Published PORTFOLIO_DROP_ALERT for user={} drop={}%", userId, dropPercentage);
    }
}
