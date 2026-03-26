package com.jeevan.TradingApp.analytics.service;

import com.jeevan.TradingApp.analytics.model.PortfolioSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Computes risk metrics from portfolio snapshot history.
 *
 * Metrics:
 *   - Max Drawdown: largest peak-to-trough decline (%)
 *   - Volatility: standard deviation of daily returns
 *   - Risk Score: composite 1-100 score based on drawdown + volatility
 */
@Service
public class RiskCalculator {

    /**
     * Max Drawdown = largest percentage decline from a peak to a subsequent trough.
     *
     * Example: peak=10000, trough=7000 → drawdown = 30%
     */
    public BigDecimal calculateMaxDrawdown(List<PortfolioSnapshot> snapshots) {
        if (snapshots == null || snapshots.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal peak = snapshots.get(0).getPortfolioValue();
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (PortfolioSnapshot snapshot : snapshots) {
            BigDecimal value = snapshot.getPortfolioValue();
            if (value.compareTo(peak) > 0) {
                peak = value;
            }

            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = peak.subtract(value)
                        .divide(peak, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }

        return maxDrawdown.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Volatility = standard deviation of daily return percentages.
     *
     * Uses the dailyReturn field stored on each snapshot.
     */
    public BigDecimal calculateVolatility(List<PortfolioSnapshot> snapshots) {
        if (snapshots == null || snapshots.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Collect daily returns
        double[] returns = snapshots.stream()
                .mapToDouble(s -> s.getDailyReturn().doubleValue())
                .toArray();

        // Mean
        double sum = 0;
        for (double r : returns) sum += r;
        double mean = sum / returns.length;

        // Variance
        double varianceSum = 0;
        for (double r : returns) {
            varianceSum += Math.pow(r - mean, 2);
        }
        double variance = varianceSum / returns.length;
        double stdDev = Math.sqrt(variance);

        return BigDecimal.valueOf(stdDev).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Simple composite risk score from 1 (low risk) to 100 (high risk).
     *
     * Formula:
     *   drawdownScore = min(maxDrawdown, 50) * 1.0   → 0-50 points
     *   volatilityScore = min(volatility * 5, 50)    → 0-50 points
     *   riskScore = drawdownScore + volatilityScore
     */
    public int calculateRiskScore(BigDecimal maxDrawdown, BigDecimal volatility) {
        double drawdownScore = Math.min(maxDrawdown.doubleValue(), 50.0);
        double volatilityScore = Math.min(volatility.doubleValue() * 5.0, 50.0);

        int score = (int) Math.round(drawdownScore + volatilityScore);
        return Math.max(1, Math.min(score, 100));
    }

    /**
     * Maps risk score to a human-readable level.
     */
    public String getRiskLevel(int riskScore) {
        if (riskScore <= 30) return "LOW";
        if (riskScore <= 60) return "MEDIUM";
        return "HIGH";
    }
}
