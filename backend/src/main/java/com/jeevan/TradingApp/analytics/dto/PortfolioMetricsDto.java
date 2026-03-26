package com.jeevan.TradingApp.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioMetricsDto {
    private BigDecimal totalInvested;
    private BigDecimal currentPortfolioValue;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal totalPnl;           // realized + unrealized
    private BigDecimal totalPnlPercentage; // (totalPnl / totalInvested) * 100
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
}
