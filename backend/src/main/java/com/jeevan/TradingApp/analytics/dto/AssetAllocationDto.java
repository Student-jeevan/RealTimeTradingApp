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
public class AssetAllocationDto {
    private String coinId;
    private String coinSymbol;
    private BigDecimal currentValue;
    private BigDecimal percentage;
    private double quantity;
}
