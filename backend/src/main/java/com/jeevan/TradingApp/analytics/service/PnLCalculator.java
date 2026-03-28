package com.jeevan.TradingApp.analytics.service;

import com.jeevan.TradingApp.analytics.repository.TradeAnalyticsRepository;
import com.jeevan.TradingApp.modal.Asset;
import com.jeevan.TradingApp.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Computes realized and unrealized PnL using FIFO cost-basis method.
 *
 * Realized PnL  = sum of (sellPrice - avgCostBasis) * qty for all SELL trades
 * Unrealized PnL = sum of (currentPrice - avgCostBasis) * holdingQty for all open positions
 */
@Service
public class PnLCalculator {

    private static final Logger log = LoggerFactory.getLogger(PnLCalculator.class);

    @Autowired
    private TradeAnalyticsRepository tradeAnalyticsRepo;


    @Autowired
    private AssetRepository assetRepository;

    /**
     * Compute realized PnL for a SELL trade.
     * Uses the average cost basis from previous BUY trades for the same coin.
     *
     * @param userId   the user
     * @param coinId   the coin sold
     * @param sellQty  quantity sold
     * @param sellPrice price at which sold
     * @return profitLoss = (sellPrice - avgCostBasis) * sellQty
     */
    public BigDecimal calculateRealizedPnlForSell(Long userId, String coinId,
                                                   double sellQty, BigDecimal sellPrice) {
        BigDecimal totalBuyCost = tradeAnalyticsRepo.sumBuyCostByUserAndCoin(userId, coinId);
        double totalBuyQty = tradeAnalyticsRepo.sumBuyQuantityByUserAndCoin(userId, coinId);

        if (totalBuyQty == 0) {
            return BigDecimal.ZERO;
        }

        // Average cost basis = total cost / total qty bought
        BigDecimal avgCostBasis = totalBuyCost.divide(
                BigDecimal.valueOf(totalBuyQty), 8, RoundingMode.HALF_UP);

        // PnL = (sellPrice - avgCost) * qty
        return sellPrice.subtract(avgCostBasis)
                .multiply(BigDecimal.valueOf(sellQty))
                .setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Compute the average cost basis for a user's holdings of a specific coin.
     */
    public BigDecimal getAverageCostBasis(Long userId, String coinId) {
        BigDecimal totalBuyCost = tradeAnalyticsRepo.sumBuyCostByUserAndCoin(userId, coinId);
        double totalBuyQty = tradeAnalyticsRepo.sumBuyQuantityByUserAndCoin(userId, coinId);

        if (totalBuyQty == 0) {
            return BigDecimal.ZERO;
        }

        return totalBuyCost.divide(
                BigDecimal.valueOf(totalBuyQty), 8, RoundingMode.HALF_UP);
    }

    /**
     * Compute total unrealized PnL across all open positions for a user.
     *
     * FIX N+1: Previously called getAverageCostBasis() per asset in a loop
     * (2 queries × N assets). Now loads all cost bases in a single aggregated query.
     *
     * @param userId       the user
     * @param currentPrices map of coinId -> currentPrice
     * @return sum of (currentPrice - avgCostBasis) * holdingQty
     */
    public BigDecimal calculateTotalUnrealizedPnl(Long userId, Map<String, Double> currentPrices) {
        List<Asset> assets = assetRepository.findByUserId(userId);
        if (assets.isEmpty()) return BigDecimal.ZERO;

        // Single query: get avg cost basis for all coins this user has bought
        Map<String, BigDecimal> avgCostBasisMap = new HashMap<>();
        try {
            List<Object[]> rows = tradeAnalyticsRepo.getAvgCostBasisPerCoin(userId);
            for (Object[] row : rows) {
                String coinId = (String) row[0];
                BigDecimal avgCost = row[1] instanceof BigDecimal bd
                        ? bd
                        : BigDecimal.valueOf(((Number) row[1]).doubleValue());
                avgCostBasisMap.put(coinId, avgCost);
            }
        } catch (Exception e) {
            // Fallback: use per-asset query if batched query fails
            log.warn("[PnLCalculator] Batched cost-basis query failed, falling back to per-asset queries: {}", e.getMessage());
        }

        BigDecimal totalUnrealized = BigDecimal.ZERO;

        for (Asset asset : assets) {
            String coinId = asset.getCoin().getId();
            double holdingQty = asset.getQuantity();
            if (holdingQty <= 0) continue;

            Double currentPrice = currentPrices.get(coinId);
            if (currentPrice == null) continue;

            // Use batched result; fall back to individual query if not present
            BigDecimal avgCost = avgCostBasisMap.computeIfAbsent(coinId,
                    id -> getAverageCostBasis(userId, id));

            if (avgCost.compareTo(BigDecimal.ZERO) == 0) continue;

            // unrealized = (currentPrice - avgCost) * qty
            BigDecimal unrealized = BigDecimal.valueOf(currentPrice)
                    .subtract(avgCost)
                    .multiply(BigDecimal.valueOf(holdingQty));

            totalUnrealized = totalUnrealized.add(unrealized);
        }

        return totalUnrealized.setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Compute the current portfolio value given live prices.
     *
     * @param userId       the user
     * @param currentPrices map of coinId -> currentPrice
     * @return sum of (currentPrice * holdingQty)
     */
    public BigDecimal calculatePortfolioValue(Long userId, Map<String, Double> currentPrices) {
        List<Asset> assets = assetRepository.findByUserId(userId);
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Asset asset : assets) {
            String coinId = asset.getCoin().getId();
            double holdingQty = asset.getQuantity();
            Double price = currentPrices.get(coinId);

            if (price != null && holdingQty > 0) {
                totalValue = totalValue.add(
                        BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(holdingQty)));
            }
        }

        return totalValue.setScale(8, RoundingMode.HALF_UP);
    }
}
