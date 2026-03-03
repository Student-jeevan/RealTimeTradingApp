package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.modal.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

    List<WalletLedger> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT SUM(wl.amount) FROM WalletLedger wl WHERE wl.user.id = :userId AND wl.transactionType IN :types")
    Optional<BigDecimal> sumAmountByUserIdAndTransactionTypes(@Param("userId") Long userId,
            @Param("types") List<LedgerTransactionType> types);
}
