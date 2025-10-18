package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal , Long> {
    List<Withdrawal> findByUserId(Long userId);
}
