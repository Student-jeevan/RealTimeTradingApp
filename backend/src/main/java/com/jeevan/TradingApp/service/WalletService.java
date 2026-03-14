package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;

public interface WalletService {
    Wallet getUserWallet(User user);

    Wallet addBalance(User user, java.math.BigDecimal money);

    Wallet findWalletById(Long id);

    Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, Long amount);

    Wallet payOrderPayment(Order order, User user);

    Wallet debit(User user, java.math.BigDecimal amount, String referenceId, String description);

    Wallet credit(User user, java.math.BigDecimal amount, String referenceId, String description);

    Wallet releaseLock(User user, java.math.BigDecimal amount, String referenceId, String description);
}
