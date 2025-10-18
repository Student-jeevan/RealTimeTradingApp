package com.jeevan.TradingApp.request;

import com.jeevan.TradingApp.domain.OrderType;
import lombok.Data;

@Data
public class CreateOrderRequest {
    private String coinId;
    private double quantity;
    private OrderType orderType;
}
