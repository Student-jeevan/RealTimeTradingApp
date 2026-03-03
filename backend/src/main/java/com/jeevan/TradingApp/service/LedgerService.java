package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.WalletLedger;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerService {
    WalletLedger createLedgerEntry(User user, LedgerTransactionType type, BigDecimal amount, String referenceId,
            String description);

    BigDecimal calculateAvailableBalance(Long userId);

    List<WalletLedger> getUserLedger(Long userId);
}
