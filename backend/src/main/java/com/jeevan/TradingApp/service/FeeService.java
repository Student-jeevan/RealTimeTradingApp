package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.User;

import java.math.BigDecimal;

public interface FeeService {
    BigDecimal calculateFee(BigDecimal tradeAmount);

    void deductFee(User user, BigDecimal amount, String referenceId, String description) throws Exception;
}
