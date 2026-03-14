package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.exception.RiskValidationException;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.request.CreateOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskValidationServiceImpl implements RiskValidationService {

    @Autowired
    private LedgerService ledgerService;

    // Adding basic Redis Rate limiter mockup logic (Ideally using a proper Spring
    // Redis integration)
    // For now, implementing the pure business checks requested.

    @Override
    public void validateTrade(User user, CreateOrderRequest request, double currentPrice) {
        BigDecimal tradeValue = BigDecimal.valueOf(request.getQuantity() * currentPrice);

        // 1. Minimum Trade Value (e.g., ₹100 or $1 depending on base currency. Assuming
        // $1 scale for crypto)
        BigDecimal minimumTradeValue = BigDecimal.valueOf(1.0); // Adjust to requirements (e.g., 100 if INR)
        if (tradeValue.compareTo(minimumTradeValue) < 0) {
            throw new RiskValidationException("Trade value must be at least " + minimumTradeValue);
        }

        // 2. Maximum Allocation Rule (Cannot use > 80% of available wallet balance for
        // BUY)
        if (request.getOrderType() == OrderType.BUY) {
            BigDecimal availableBalance = ledgerService.calculateAvailableBalance(user.getId());
            System.out.println("RiskValidation - userId=" + user.getId()
                    + ", availableBalance(before trade)=" + availableBalance
                    + ", requestedTradeValue=" + tradeValue);
            BigDecimal maxAllowedAllocation = availableBalance.multiply(BigDecimal.valueOf(0.8));

            if (tradeValue.compareTo(maxAllowedAllocation) > 0) {
                throw new RiskValidationException("Cannot allocate more than 80% of available balance per trade.");
            }

            // 3. Insufficient balance check
            if (tradeValue.compareTo(availableBalance) > 0) {
                throw new RiskValidationException("Insufficient available balance for this trade.");
            }
        }

        // (Optional for SELL: Check if user has sufficient crypto quantity in
        // AssetService)
    }
}
