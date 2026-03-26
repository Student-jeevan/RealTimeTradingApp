package com.jeevan.TradingApp.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Aggregate portfolio and trade metrics per user.
 * Updated incrementally by analytics Kafka consumers.
 */
@Entity
@Table(name = "user_analytics",
       indexes = @Index(name = "idx_user_analytics_user", columnList = "userId", unique = true))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // ---- Portfolio Metrics ----
    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal currentPortfolioValue = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    // ---- Trade Performance ----
    @Builder.Default
    private int totalTrades = 0;

    @Builder.Default
    private int winCount = 0;

    @Builder.Default
    private int lossCount = 0;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal bestTradeProfit = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal worstTradeProfit = BigDecimal.ZERO;

    // ---- Risk Metrics ----
    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal maxDrawdown = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal peakPortfolioValue = BigDecimal.ZERO;

    @Column(precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal volatility = BigDecimal.ZERO;

    @Builder.Default
    private int riskScore = 0;

    // ---- Cash Flow ----
    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
