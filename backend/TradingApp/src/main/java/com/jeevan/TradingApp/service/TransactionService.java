package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.WalletTransactionType;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.WalletTransaction;

import java.time.LocalDate;
import java.util.List;

public interface TransactionService {
    List<WalletTransaction> getTransactionsByWalletId(Wallet wallet);
       WalletTransaction createTransaction( Wallet wallet, WalletTransactionType type, String transferId, String purpose, Long amount);
}
