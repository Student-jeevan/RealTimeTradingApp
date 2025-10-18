package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository  extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletId(Long walletId);
}
