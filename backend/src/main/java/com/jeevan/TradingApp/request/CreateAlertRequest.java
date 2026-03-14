package com.jeevan.TradingApp.request;

import com.jeevan.TradingApp.domain.AlertCondition;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAlertRequest {
    private String coinId;
    private BigDecimal targetPrice;
    private AlertCondition condition;
}
