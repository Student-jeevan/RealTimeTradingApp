package com.jeevan.TradingApp.analytics.controller;

import com.jeevan.TradingApp.analytics.dto.*;
import com.jeevan.TradingApp.analytics.service.AnalyticsService;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for analytics endpoints.
 * All endpoints require JWT authentication (handled by JwtTokenValidator filter).
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserService userService;

    /**
     * Portfolio overview: total invested, current value, PnL breakdown.
     *
     * GET /api/analytics/portfolio
     *
     * Sample response:
     * {
     *   "totalInvested": 10000.00,
     *   "currentPortfolioValue": 12500.00,
     *   "realizedPnl": 1500.00,
     *   "unrealizedPnl": 1000.00,
     *   "totalPnl": 2500.00,
     *   "totalPnlPercentage": 25.00,
     *   "totalDeposits": 15000.00,
     *   "totalWithdrawals": 5000.00
     * }
     */
    @GetMapping("/portfolio")
    public ResponseEntity<PortfolioMetricsDto> getPortfolioMetrics(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        PortfolioMetricsDto metrics = analyticsService.getPortfolioMetrics(user.getId());
        return ResponseEntity.ok(metrics);
    }

    /**
     * Trade performance: win rate, best/worst trade, average profit.
     *
     * GET /api/analytics/performance
     *
     * Sample response:
     * {
     *   "totalTrades": 42,
     *   "winCount": 28,
     *   "lossCount": 14,
     *   "winRate": 66.67,
     *   "avgProfitPerTrade": 150.50,
     *   "bestTradeProfit": 2340.00,
     *   "worstTradeProfit": -890.00,
     *   "totalRealizedPnl": 4214.00
     * }
     */
    @GetMapping("/performance")
    public ResponseEntity<TradePerformanceDto> getTradePerformance(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        TradePerformanceDto performance = analyticsService.getTradePerformance(user.getId());
        return ResponseEntity.ok(performance);
    }

    /**
     * Risk analysis: drawdown, volatility, risk score.
     *
     * GET /api/analytics/risk
     *
     * Sample response:
     * {
     *   "maxDrawdown": 15.4200,
     *   "volatility": 3.2500,
     *   "riskScore": 32,
     *   "riskLevel": "MEDIUM"
     * }
     */
    @GetMapping("/risk")
    public ResponseEntity<RiskMetricsDto> getRiskMetrics(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        RiskMetricsDto risk = analyticsService.getRiskMetrics(user.getId());
        return ResponseEntity.ok(risk);
    }

    /**
     * Asset allocation: percentage breakdown by coin.
     *
     * GET /api/analytics/allocation
     *
     * Sample response:
     * [
     *   { "coinId": "bitcoin", "coinSymbol": "btc", "currentValue": 8500.00, "percentage": 68.00, "quantity": 0.1 },
     *   { "coinId": "ethereum", "coinSymbol": "eth", "currentValue": 4000.00, "percentage": 32.00, "quantity": 2.5 }
     * ]
     */
    @GetMapping("/allocation")
    public ResponseEntity<List<AssetAllocationDto>> getAssetAllocation(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        List<AssetAllocationDto> allocation = analyticsService.getAssetAllocation(user.getId());
        return ResponseEntity.ok(allocation);
    }
}
