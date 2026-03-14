package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.modal.SystemRevenue;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.SystemRevenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeServiceImpl implements FeeService {

    @Value("${trading.fee.percentage:0.1}")
    private BigDecimal feePercentage;

    @Autowired
    private SystemRevenueRepository systemRevenueRepository;

    @Autowired
    private LedgerService ledgerService;

    @Override
    public BigDecimal calculateFee(BigDecimal tradeAmount) {
        if (tradeAmount == null || tradeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return tradeAmount.multiply(feePercentage)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public void deductFee(User user, BigDecimal amount, String referenceId, String description) {
        BigDecimal fee = calculateFee(amount);
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            // 1. Add to Ledger
            ledgerService.createLedgerEntry(user, LedgerTransactionType.FEE, fee, referenceId, description);

            // 2. Add to System Revenue
            SystemRevenue revenue = new SystemRevenue();
            revenue.setAmount(fee);
            systemRevenueRepository.save(revenue);
        }
    }
}
