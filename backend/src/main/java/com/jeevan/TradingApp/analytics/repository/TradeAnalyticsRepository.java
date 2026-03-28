package com.jeevan.TradingApp.analytics.repository;

import com.jeevan.TradingApp.analytics.model.TradeAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TradeAnalyticsRepository extends JpaRepository<TradeAnalytics, Long> {

    List<TradeAnalytics> findByUserId(Long userId);

    List<TradeAnalytics> findByUserIdAndCoinId(Long userId, String coinId);

    List<TradeAnalytics> findByUserIdOrderByTimestampDesc(Long userId);

    Optional<TradeAnalytics> findByOrderId(Long orderId);

    /** Finds BUY trades for a user+coin ordered oldest-first (FIFO cost basis). */
    List<TradeAnalytics> findByUserIdAndCoinIdAndOrderTypeOrderByTimestampAsc(
            Long userId, String coinId, String orderType);

    /** Sum of (price * quantity) for all BUY trades of a given user+coin. */
    @Query("SELECT COALESCE(SUM(t.price * t.quantity), 0) FROM TradeAnalytics t " +
           "WHERE t.userId = :userId AND t.coinId = :coinId AND t.orderType = 'BUY'")
    BigDecimal sumBuyCostByUserAndCoin(@Param("userId") Long userId, @Param("coinId") String coinId);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM TradeAnalytics t " +
           "WHERE t.userId = :userId AND t.coinId = :coinId AND t.orderType = 'BUY'")
    double sumBuyQuantityByUserAndCoin(@Param("userId") Long userId, @Param("coinId") String coinId);

    /**
     * FIX N+1: Returns avg cost basis per coin in a single aggregated query.
     * Result: List of Object[] with [coinId (String), avgCostBasis (Double)]
     *
     * Replaces the previous pattern of calling sumBuyCostByUserAndCoin +
     * sumBuyQuantityByUserAndCoin in a per-asset loop (2N queries → 1 query).
     */
    @Query("SELECT t.coinId, " +
           "       CASE WHEN SUM(t.quantity) > 0 " +
           "            THEN SUM(t.price * t.quantity) / SUM(t.quantity) " +
           "            ELSE 0 END " +
           "FROM TradeAnalytics t " +
           "WHERE t.userId = :userId AND t.orderType = 'BUY' " +
           "GROUP BY t.coinId")
    List<Object[]> getAvgCostBasisPerCoin(@Param("userId") Long userId);
}
