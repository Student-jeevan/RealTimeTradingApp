package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.request.CreateOrderRequest;

public interface RiskValidationService {
    void validateTrade(User user, CreateOrderRequest request, double currentPrice);
}
