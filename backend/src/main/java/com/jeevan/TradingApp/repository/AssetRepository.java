package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByUserId(Long userId);
    Asset findByUserIdAndCoinId(Long userId, String coinId);

    /**
     * Returns distinct user IDs who currently hold a given coin.
     * Used by AnalyticsPriceConsumer to limit recomputation to affected users only.
     */
    @Query("SELECT DISTINCT a.user.id FROM Asset a WHERE a.coin.id = :coinId AND a.quantity > 0")
    List<Long> findUserIdsByCoinId(@Param("coinId") String coinId);
}
