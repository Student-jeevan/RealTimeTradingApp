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
public class RiskMetricsDto {
    private BigDecimal maxDrawdown;        // percentage
    private BigDecimal volatility;         // std dev of daily returns
    private int riskScore;                 // 1-100
    private String riskLevel;              // LOW, MEDIUM, HIGH
}
