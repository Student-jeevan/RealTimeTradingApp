package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;

public interface WalletService {
    Wallet getUserWallet(User user);

    Wallet addBalance(Wallet wallet, java.math.BigDecimal money);

    Wallet findWalletById(Long id) throws Exception;

    Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, Long amount) throws Exception;

    Wallet payOrderPayment(Order order, User user) throws Exception;
}
