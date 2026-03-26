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
public class TradePerformanceDto {
    private int totalTrades;
    private int winCount;
    private int lossCount;
    private BigDecimal winRate;          // (winCount / totalSellTrades) * 100
    private BigDecimal avgProfitPerTrade;
    private BigDecimal bestTradeProfit;
    private BigDecimal worstTradeProfit;
    private BigDecimal totalRealizedPnl;
}
