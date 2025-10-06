package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Wallet findByUserId(Long userId);
}
