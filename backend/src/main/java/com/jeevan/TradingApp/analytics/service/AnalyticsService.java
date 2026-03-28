package com.jeevan.TradingApp.analytics.service;

import com.jeevan.TradingApp.analytics.dto.*;
import com.jeevan.TradingApp.analytics.model.*;
import com.jeevan.TradingApp.analytics.repository.*;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.modal.Asset;
import com.jeevan.TradingApp.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core analytics orchestration service.
 *
 * Maintains per-user analytics state, computes metrics on demand,
 * and pushes real-time updates via WebSocket.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String PRICE_CACHE_KEY = "analytics:prices";

    // Redis key pattern for per-user portfolio cache (5-second TTL)
    private static final String PORTFOLIO_CACHE_PREFIX = "user:%d:portfolio";
    private static final long PORTFOLIO_CACHE_TTL_SECONDS = 5;

    @Autowired private UserAnalyticsRepository userAnalyticsRepo;
    @Autowired private TradeAnalyticsRepository tradeAnalyticsRepo;
    @Autowired private PortfolioSnapshotRepository snapshotRepo;
    @Autowired private AnalyticsProcessedEventRepository processedEventRepo;
    @Autowired private AssetRepository assetRepository;
    @Autowired private PnLCalculator pnlCalculator;
    @Autowired private RiskCalculator riskCalculator;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    /**
     * In-memory price cache for fast lookups.
     * Updated by AnalyticsPriceConsumer on each price-update event.
     */
    private final ConcurrentHashMap<String, Double> latestPrices = new ConcurrentHashMap<>();

    // ============================================================
    // EVENT PROCESSING (called by Kafka consumers)
    // ============================================================

    /**
     * Process a trade event: record the trade and update aggregate metrics.
     */
    @Transactional
    public void processTradeEvent(TradeEvent event) {
        // Only process FILLED trades
        if (!"FILLED".equalsIgnoreCase(event.getStatus())
                && !"SUCCESS".equalsIgnoreCase(event.getStatus())) {
            log.debug("Skipping non-filled trade event: {}", event.getEventId());
            return;
        }

        UserAnalytics analytics = getOrCreateUserAnalytics(event.getUserId());

        BigDecimal tradeValue = event.getPrice().multiply(BigDecimal.valueOf(event.getQuantity()));
        BigDecimal profitLoss = BigDecimal.ZERO;
        BigDecimal costBasis = event.getPrice();

        if ("BUY".equalsIgnoreCase(event.getOrderType())) {
            // BUY: increase total invested
            analytics.setTotalInvested(analytics.getTotalInvested().add(tradeValue));
        } else if ("SELL".equalsIgnoreCase(event.getOrderType())) {
            // SELL: compute realized PnL
            costBasis = pnlCalculator.getAverageCostBasis(event.getUserId(), event.getCoinId());
            profitLoss = pnlCalculator.calculateRealizedPnlForSell(
                    event.getUserId(), event.getCoinId(),
                    event.getQuantity(), event.getPrice());

            analytics.setRealizedPnl(analytics.getRealizedPnl().add(profitLoss));

            // Update win/loss counters
            if (profitLoss.compareTo(BigDecimal.ZERO) > 0) {
                analytics.setWinCount(analytics.getWinCount() + 1);
            } else if (profitLoss.compareTo(BigDecimal.ZERO) < 0) {
                analytics.setLossCount(analytics.getLossCount() + 1);
            }

            // Track best/worst trade
            if (profitLoss.compareTo(analytics.getBestTradeProfit()) > 0) {
                analytics.setBestTradeProfit(profitLoss);
            }
            if (profitLoss.compareTo(analytics.getWorstTradeProfit()) < 0) {
                analytics.setWorstTradeProfit(profitLoss);
            }
        }

        analytics.setTotalTrades(analytics.getTotalTrades() + 1);

        // Record individual trade
        TradeAnalytics trade = TradeAnalytics.builder()
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .orderType(event.getOrderType())
                .coinId(event.getCoinId())
                .coinSymbol(event.getCoinSymbol())
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .costBasis(costBasis)
                .profitLoss(profitLoss)
                .timestamp(event.getTimestamp())
                .build();

        tradeAnalyticsRepo.save(trade);
        userAnalyticsRepo.save(analytics);

        // FIX: Invalidate Redis cache after trade so next GET reflects new state
        evictPortfolioCache(event.getUserId());

        log.info("[Analytics] Processed trade for user={} coin={} type={} pnl={}",
                event.getUserId(), event.getCoinSymbol(), event.getOrderType(), profitLoss);
    }

    /**
     * Process a transaction event: track cash deposits/withdrawals.
     */
    @Transactional
    public void processTransactionEvent(TransactionEvent event) {
        UserAnalytics analytics = getOrCreateUserAnalytics(event.getUserId());

        switch (event.getTransactionType().toUpperCase()) {
            case "CREDIT":
                analytics.setTotalDeposits(analytics.getTotalDeposits().add(event.getAmount()));
                break;
            case "DEBIT":
                analytics.setTotalWithdrawals(analytics.getTotalWithdrawals().add(event.getAmount()));
                break;
            // TRADE_LOCK, TRADE_RELEASE, FEE — tracked but not counted as deposits/withdrawals
            default:
                log.debug("Transaction type {} not counted for cash flow.", event.getTransactionType());
        }

        userAnalyticsRepo.save(analytics);
    }

    /**
     * Process a price update: cache latest price and recompute valuations.
     */
    public void processPriceUpdate(String coinId, double currentPrice) {
        latestPrices.put(coinId, currentPrice);

        // Also cache in Redis for persistence across restarts
        try {
            redisTemplate.opsForHash().put(PRICE_CACHE_KEY, coinId, currentPrice);
        } catch (Exception e) {
            log.warn("Failed to cache price in Redis: {}", e.getMessage());
        }
    }

    /**
     * Recompute portfolio value and unrealized PnL for a user after price changes.
     * Pushes update via WebSocket.
     */
    @Transactional
    public void recomputePortfolioForUser(Long userId) {
        UserAnalytics analytics = getOrCreateUserAnalytics(userId);
        Map<String, Double> prices = getCurrentPrices();

        BigDecimal portfolioValue = pnlCalculator.calculatePortfolioValue(userId, prices);
        BigDecimal unrealizedPnl = pnlCalculator.calculateTotalUnrealizedPnl(userId, prices);

        analytics.setCurrentPortfolioValue(portfolioValue);
        analytics.setUnrealizedPnl(unrealizedPnl);

        // Update peak for drawdown calculation
        if (portfolioValue.compareTo(analytics.getPeakPortfolioValue()) > 0) {
            analytics.setPeakPortfolioValue(portfolioValue);
        }

        // Update max drawdown
        if (analytics.getPeakPortfolioValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal drawdown = analytics.getPeakPortfolioValue().subtract(portfolioValue)
                    .divide(analytics.getPeakPortfolioValue(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (drawdown.compareTo(analytics.getMaxDrawdown()) > 0) {
                analytics.setMaxDrawdown(drawdown);
            }
        }

        userAnalyticsRepo.save(analytics);

        // FIX: Only take snapshot once per day — not on every price tick.
        // Previously called unconditionally, causing a DB write per price update per user.
        if (!snapshotRepo.findByUserIdAndSnapshotDate(userId, LocalDate.now()).isPresent()) {
            takePortfolioSnapshot(userId, portfolioValue, analytics.getTotalInvested());
        }

        // Update Redis cache with fresh values (5s TTL)
        PortfolioMetricsDto metrics = buildPortfolioMetrics(analytics);
        try {
            String cacheKey = String.format(PORTFOLIO_CACHE_PREFIX, userId);
            redisTemplate.opsForValue().set(cacheKey, metrics,
                    PORTFOLIO_CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Analytics] Failed to update Redis cache for user {}: {}", userId, e.getMessage());
        }

        // Push via WebSocket
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/analytics",
                    metrics);
        } catch (Exception e) {
            log.warn("Failed to push analytics via WebSocket for user {}: {}", userId, e.getMessage());
        }
    }

    // ============================================================
    // API METHODS (called by AnalyticsController)
    // ============================================================

    /**
     * GET /api/analytics/portfolio
     *
     * FIX: Reads from Redis cache (5s TTL) before hitting DB.
     * Cache is invalidated on trade events so new trades appear immediately.
     * On cache miss, recomputes and writes back to Redis.
     */
    public PortfolioMetricsDto getPortfolioMetrics(Long userId) {
        String cacheKey = String.format(PORTFOLIO_CACHE_PREFIX, userId);

        // 1. Try cache first
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PortfolioMetricsDto dto) {
                log.debug("[Analytics] CACHE HIT for portfolio userId={}", userId);
                return dto;
            }
        } catch (Exception e) {
            log.warn("[Analytics] Redis cache read failed for userId={}: {}", userId, e.getMessage());
        }

        log.debug("[Analytics] CACHE MISS for portfolio userId={}, recomputing", userId);

        // 2. Cache miss — load from DB and recompute
        UserAnalytics analytics = getOrCreateUserAnalytics(userId);

        Map<String, Double> prices = getCurrentPrices();
        if (!prices.isEmpty()) {
            BigDecimal portfolioValue = pnlCalculator.calculatePortfolioValue(userId, prices);
            BigDecimal unrealizedPnl = pnlCalculator.calculateTotalUnrealizedPnl(userId, prices);
            analytics.setCurrentPortfolioValue(portfolioValue);
            analytics.setUnrealizedPnl(unrealizedPnl);
        }

        PortfolioMetricsDto result = buildPortfolioMetrics(analytics);

        // 3. Write back to cache
        try {
            redisTemplate.opsForValue().set(cacheKey, result,
                    PORTFOLIO_CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Analytics] Redis cache write failed for userId={}: {}", userId, e.getMessage());
        }

        return result;
    }

    /**
     * GET /api/analytics/performance
     */
    public TradePerformanceDto getTradePerformance(Long userId) {
        UserAnalytics analytics = getOrCreateUserAnalytics(userId);

        int totalSellTrades = analytics.getWinCount() + analytics.getLossCount();
        BigDecimal winRate = BigDecimal.ZERO;
        BigDecimal avgProfit = BigDecimal.ZERO;

        if (totalSellTrades > 0) {
            winRate = BigDecimal.valueOf(analytics.getWinCount())
                    .divide(BigDecimal.valueOf(totalSellTrades), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            avgProfit = analytics.getRealizedPnl()
                    .divide(BigDecimal.valueOf(totalSellTrades), 8, RoundingMode.HALF_UP);
        }

        return TradePerformanceDto.builder()
                .totalTrades(analytics.getTotalTrades())
                .winCount(analytics.getWinCount())
                .lossCount(analytics.getLossCount())
                .winRate(winRate)
                .avgProfitPerTrade(avgProfit)
                .bestTradeProfit(analytics.getBestTradeProfit())
                .worstTradeProfit(analytics.getWorstTradeProfit())
                .totalRealizedPnl(analytics.getRealizedPnl())
                .build();
    }

    /**
     * GET /api/analytics/risk
     */
    public RiskMetricsDto getRiskMetrics(Long userId) {
        UserAnalytics analytics = getOrCreateUserAnalytics(userId);

        // Compute volatility from last 30 days of snapshots
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        List<PortfolioSnapshot> snapshots = snapshotRepo
                .findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(userId, start, end);

        BigDecimal maxDrawdown = analytics.getMaxDrawdown();
        BigDecimal volatility = riskCalculator.calculateVolatility(snapshots);

        // Also recompute drawdown from snapshots for accuracy
        BigDecimal snapshotDrawdown = riskCalculator.calculateMaxDrawdown(snapshots);
        if (snapshotDrawdown.compareTo(maxDrawdown) > 0) {
            maxDrawdown = snapshotDrawdown;
        }

        int riskScore = riskCalculator.calculateRiskScore(maxDrawdown, volatility);
        String riskLevel = riskCalculator.getRiskLevel(riskScore);

        // Persist
        analytics.setVolatility(volatility);
        analytics.setRiskScore(riskScore);
        userAnalyticsRepo.save(analytics);

        return RiskMetricsDto.builder()
                .maxDrawdown(maxDrawdown)
                .volatility(volatility)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .build();
    }

    /**
     * GET /api/analytics/allocation
     */
    public List<AssetAllocationDto> getAssetAllocation(Long userId) {
        List<Asset> assets = assetRepository.findByUserId(userId);
        Map<String, Double> prices = getCurrentPrices();

        List<AssetAllocationDto> allocations = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        // First pass: compute each asset's value
        for (Asset asset : assets) {
            if (asset.getQuantity() <= 0) continue;

            String coinId = asset.getCoin().getId();
            Double price = prices.getOrDefault(coinId, asset.getBuyPrice());
            BigDecimal value = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(asset.getQuantity()));

            allocations.add(AssetAllocationDto.builder()
                    .coinId(coinId)
                    .coinSymbol(asset.getCoin().getSymbol())
                    .currentValue(value.setScale(2, RoundingMode.HALF_UP))
                    .quantity(asset.getQuantity())
                    .build());

            totalValue = totalValue.add(value);
        }

        // Second pass: compute percentages
        for (AssetAllocationDto alloc : allocations) {
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = alloc.getCurrentValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                alloc.setPercentage(pct.setScale(2, RoundingMode.HALF_UP));
            } else {
                alloc.setPercentage(BigDecimal.ZERO);
            }
        }

        // Sort by value descending
        allocations.sort((a, b) -> b.getCurrentValue().compareTo(a.getCurrentValue()));
        return allocations;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Get or create UserAnalytics record for a user.
     */
    public UserAnalytics getOrCreateUserAnalytics(Long userId) {
        return userAnalyticsRepo.findByUserId(userId)
                .orElseGet(() -> {
                    UserAnalytics ua = UserAnalytics.builder().userId(userId).build();
                    return userAnalyticsRepo.save(ua);
                });
    }

    /**
     * Get all known user IDs that have analytics records.
     */
    public List<Long> getAllTrackedUserIds() {
        return userAnalyticsRepo.findAll().stream()
                .map(UserAnalytics::getUserId)
                .toList();
    }

    /**
     * Check if an event has already been processed by analytics consumers.
     */
    public boolean isEventProcessed(String eventId) {
        return processedEventRepo.existsByEventId(eventId);
    }

    /**
     * Mark an event as processed.
     */
    @Transactional
    public void markEventProcessed(String eventId, String consumerGroup) {
        AnalyticsProcessedEvent pe = new AnalyticsProcessedEvent();
        pe.setEventId(eventId);
        pe.setConsumerGroup(consumerGroup);
        processedEventRepo.save(pe);
    }

    /**
     * Cache a price update.
     */
    public void updatePrice(String coinId, double price) {
        latestPrices.put(coinId, price);
    }

    /**
     * Evict the per-user portfolio metrics cache from Redis.
     * Called after a trade is processed so the next GET reflects fresh data.
     */
    private void evictPortfolioCache(Long userId) {
        try {
            String cacheKey = String.format(PORTFOLIO_CACHE_PREFIX, userId);
            redisTemplate.delete(cacheKey);
            log.debug("[Analytics] Evicted portfolio cache for userId={}", userId);
        } catch (Exception e) {
            log.warn("[Analytics] Cache eviction failed for userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get the current prices map (in-memory + Redis fallback).
     */
    public Map<String, Double> getCurrentPrices() {
        if (!latestPrices.isEmpty()) {
            return new HashMap<>(latestPrices);
        }

        // Fallback: try loading from Redis
        try {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(PRICE_CACHE_KEY);
            if (cached != null && !cached.isEmpty()) {
                cached.forEach((k, v) -> latestPrices.put(k.toString(), ((Number) v).doubleValue()));
            }
        } catch (Exception e) {
            log.warn("Failed to load prices from Redis: {}", e.getMessage());
        }

        return new HashMap<>(latestPrices);
    }

    /**
     * Take or update a daily portfolio snapshot.
     */
    private void takePortfolioSnapshot(Long userId, BigDecimal portfolioValue, BigDecimal totalInvested) {
        LocalDate today = LocalDate.now();

        PortfolioSnapshot snapshot = snapshotRepo.findByUserIdAndSnapshotDate(userId, today)
                .orElse(PortfolioSnapshot.builder()
                        .userId(userId)
                        .snapshotDate(today)
                        .build());

        // Compute daily return
        BigDecimal dailyReturn = BigDecimal.ZERO;
        Optional<PortfolioSnapshot> previousOpt = snapshotRepo.findTopByUserIdOrderBySnapshotDateDesc(userId);
        if (previousOpt.isPresent() && !previousOpt.get().getSnapshotDate().equals(today)) {
            BigDecimal prevValue = previousOpt.get().getPortfolioValue();
            if (prevValue.compareTo(BigDecimal.ZERO) > 0) {
                dailyReturn = portfolioValue.subtract(prevValue)
                        .divide(prevValue, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        snapshot.setPortfolioValue(portfolioValue);
        snapshot.setTotalInvested(totalInvested);
        snapshot.setDailyReturn(dailyReturn);

        snapshotRepo.save(snapshot);
    }

    private PortfolioMetricsDto buildPortfolioMetrics(UserAnalytics analytics) {
        BigDecimal totalPnl = analytics.getRealizedPnl().add(analytics.getUnrealizedPnl());
        BigDecimal totalPnlPct = BigDecimal.ZERO;

        if (analytics.getTotalInvested().compareTo(BigDecimal.ZERO) > 0) {
            totalPnlPct = totalPnl
                    .divide(analytics.getTotalInvested(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return PortfolioMetricsDto.builder()
                .totalInvested(analytics.getTotalInvested())
                .currentPortfolioValue(analytics.getCurrentPortfolioValue())
                .realizedPnl(analytics.getRealizedPnl())
                .unrealizedPnl(analytics.getUnrealizedPnl())
                .totalPnl(totalPnl)
                .totalPnlPercentage(totalPnlPct)
                .totalDeposits(analytics.getTotalDeposits())
                .totalWithdrawals(analytics.getTotalWithdrawals())
                .build();
    }
}
