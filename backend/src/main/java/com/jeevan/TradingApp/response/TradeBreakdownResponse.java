package com.jeevan.TradingApp.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeBreakdownResponse {
    private BigDecimal currentPrice;
    private BigDecimal tradeValue;
    private BigDecimal fee;
    private BigDecimal totalCost;
    private BigDecimal availableBalanceAfterTrade;
}
