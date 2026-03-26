package com.jeevan.TradingApp.analytics.repository;

import com.jeevan.TradingApp.analytics.model.TradeAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TradeAnalyticsRepository extends JpaRepository<TradeAnalytics, Long> {

    List<TradeAnalytics> findByUserId(Long userId);

    List<TradeAnalytics> findByUserIdAndCoinId(Long userId, String coinId);

    List<TradeAnalytics> findByUserIdOrderByTimestampDesc(Long userId);

    Optional<TradeAnalytics> findByOrderId(Long orderId);

    /**
     * Finds BUY trades for a user+coin ordered oldest-first (FIFO cost basis).
     */
    List<TradeAnalytics> findByUserIdAndCoinIdAndOrderTypeOrderByTimestampAsc(
            Long userId, String coinId, String orderType);

    /**
     * Sum of (price * quantity) for all BUY trades of a given user+coin.
     * Used to compute average cost basis.
     */
    @Query("SELECT COALESCE(SUM(t.price * t.quantity), 0) FROM TradeAnalytics t " +
           "WHERE t.userId = :userId AND t.coinId = :coinId AND t.orderType = 'BUY'")
    BigDecimal sumBuyCostByUserAndCoin(Long userId, String coinId);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM TradeAnalytics t " +
           "WHERE t.userId = :userId AND t.coinId = :coinId AND t.orderType = 'BUY'")
    double sumBuyQuantityByUserAndCoin(Long userId, String coinId);
}
