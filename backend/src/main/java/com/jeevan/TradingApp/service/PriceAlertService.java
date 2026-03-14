package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.AlertCondition;
import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.modal.User;

import java.math.BigDecimal;
import java.util.List;

public interface PriceAlertService {

    PriceAlert createAlert(User user, String coinId, BigDecimal targetPrice, AlertCondition condition);

    List<PriceAlert> getActiveAlerts(Long userId);

    List<PriceAlert> getTriggeredAlerts(Long userId);

    void deleteAlert(Long alertId, Long userId);

    void triggerAlert(PriceAlert alert, BigDecimal currentPrice);
}
