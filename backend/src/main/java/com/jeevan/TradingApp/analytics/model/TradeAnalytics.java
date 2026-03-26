package com.jeevan.TradingApp.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records each individual trade for detailed performance analysis.
 * BUY trades set entryPrice; SELL trades compute profitLoss.
 */
@Entity
@Table(name = "trade_analytics",
       indexes = {
           @Index(name = "idx_trade_analytics_user", columnList = "userId"),
           @Index(name = "idx_trade_analytics_coin", columnList = "userId, coinId"),
           @Index(name = "idx_trade_analytics_order", columnList = "orderId", unique = true)
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String orderType;   // BUY or SELL

    @Column(nullable = false)
    private String coinId;

    private String coinSymbol;

    private double quantity;

    /** Price at which this trade was executed */
    @Column(precision = 18, scale = 8)
    private BigDecimal price;

    /**
     * For SELL trades: the average buy price of the sold asset.
     * For BUY trades: same as price (entry).
     */
    @Column(precision = 18, scale = 8)
    private BigDecimal costBasis;

    /**
     * Realized profit/loss for SELL trades.
     * (price - costBasis) * quantity
     * Zero for BUY trades.
     */
    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal profitLoss = BigDecimal.ZERO;

    private LocalDateTime timestamp;
}
