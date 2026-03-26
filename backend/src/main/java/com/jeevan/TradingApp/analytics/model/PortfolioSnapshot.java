package com.jeevan.TradingApp.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily portfolio snapshot for time-series analytics (daily/weekly returns).
 * Created once per user per day; updated intra-day with the latest value.
 */
@Entity
@Table(name = "portfolio_snapshots",
       indexes = @Index(name = "idx_snapshot_user_date", columnList = "userId, snapshotDate", unique = true))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal portfolioValue = BigDecimal.ZERO;

    @Column(precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal totalInvested = BigDecimal.ZERO;

    /** Daily return percentage: ((today - yesterday) / yesterday) * 100 */
    @Column(precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal dailyReturn = BigDecimal.ZERO;
}
