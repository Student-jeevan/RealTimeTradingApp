package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.domain.WithdrawalStatus;
import com.jeevan.TradingApp.modal.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByUserId(Long userId);

    /** Returns all withdrawals with PENDING status first, then others ordered by date desc. */
    @Query("SELECT w FROM Withdrawal w ORDER BY CASE WHEN w.status = 'PENDING' THEN 0 ELSE 1 END, w.date DESC")
    List<Withdrawal> findAllOrderByPendingFirst();

    List<Withdrawal> findByStatus(WithdrawalStatus status);
}
