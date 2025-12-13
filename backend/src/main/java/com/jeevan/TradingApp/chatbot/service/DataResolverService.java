package com.jeevan.TradingApp.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.chatbot.domain.ChatIntent;
import com.jeevan.TradingApp.chatbot.dto.IntentResult;
import com.jeevan.TradingApp.modal.*;
import com.jeevan.TradingApp.service.AssetService;
import com.jeevan.TradingApp.service.CoinService;
import com.jeevan.TradingApp.service.OrderService;
import com.jeevan.TradingApp.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for resolving real-time platform data based on user intent
 * Uses existing services to fetch user-scoped data and cryptocurrency information
 */
@Service
public class DataResolverService {

    private static final Logger logger = LoggerFactory.getLogger(DataResolverService.class);

    private final WalletService walletService;
    private final AssetService assetService;
    private final OrderService orderService;
    private final CoinService coinService;
    private final ObjectMapper objectMapper;

    public DataResolverService(
            WalletService walletService,
            AssetService assetService,
            OrderService orderService,
            CoinService coinService,
            ObjectMapper objectMapper
    ) {
        this.walletService = walletService;
        this.assetService = assetService;
        this.orderService = orderService;
        this.coinService = coinService;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves relevant data based on the detected intent and cryptocurrency
     *
     * @param intentResult detected intent result containing intent and cryptocurrency
     * @param user authenticated user
     * @return map of resolved data
     */
    public Map<String, Object> resolveData(IntentResult intentResult, User user) {
        Map<String, Object> data = new HashMap<>();
        ChatIntent intent = intentResult.getIntent();
        String coinId = intentResult.getCoinId();
        String detectedCrypto = intentResult.getDetectedCryptocurrency();

        try {
            // If cryptocurrency is detected, fetch crypto data first
            if (coinId != null || detectedCrypto != null) {
                resolveCryptocurrencyData(data, coinId, detectedCrypto, user);
            }

            // Resolve intent-specific data
            switch (intent) {
                case WALLET_QUERY:
                    resolveWalletData(data, user);
                    break;
                case ASSET_QUERY:
                    resolveAssetData(data, user, coinId);
                    break;
                case ORDER_QUERY:
                    resolveOrderData(data, user, coinId);
                    break;
                case TRADE_QUERY:
                    resolveTradeData(data, user, coinId);
                    break;
                case GENERAL_QUERY:
                    resolveGeneralData(data, user);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error resolving data for intent: {}", intent, e);
            data.put("error", "Unable to fetch data: " + e.getMessage());
        }

        return data;
    }

    /**
     * Resolves relevant data based on the detected intent (backward compatibility)
     *
     * @param intent detected chat intent
     * @param user authenticated user
     * @return map of resolved data
     */
    public Map<String, Object> resolveData(ChatIntent intent, User user) {
        return resolveData(new IntentResult(intent, null, null), user);
    }

    /**
     * Resolves wallet-related data
     */
    private void resolveWalletData(Map<String, Object> data, User user) {
        Wallet wallet = walletService.getUserWallet(user);
        data.put("wallet", Map.of(
                "balance", wallet.getBalance() != null ? wallet.getBalance().toString() : "0",
                "walletId", wallet.getId() != null ? wallet.getId().toString() : "N/A"
        ));
    }

    /**
     * Resolves cryptocurrency data (price, market cap, volume, user holdings)
     */
    private void resolveCryptocurrencyData(Map<String, Object> data, String coinId, String detectedCrypto, User user) {
        try {
            Coin coin = null;
            
            // Try to find coin by ID first
            if (coinId != null) {
                try {
                    coin = coinService.findById(coinId);
                } catch (Exception e) {
                    logger.debug("Coin not found in database by ID: {}, trying to fetch from API", coinId);
                }
            }

            // If coin not in database, try to fetch from API
            if (coin == null && coinId != null) {
                try {
                    // Fetch coin details from API (this saves to database)
                    coinService.getCoinDetails(coinId);
                    coin = coinService.findById(coinId); // Should be saved now
                } catch (Exception e) {
                    logger.warn("Could not fetch coin details from API for: {}", coinId, e);
                }
            }

            // If still no coin found, try search
            if (coin == null && detectedCrypto != null) {
                try {
                    String searchResult = coinService.searchCoin(detectedCrypto);
                    JsonNode searchJson = objectMapper.readTree(searchResult);
                    JsonNode coinsArray = searchJson.get("coins");
                    if (coinsArray != null && coinsArray.isArray() && coinsArray.size() > 0) {
                        JsonNode firstCoin = coinsArray.get(0);
                        String foundCoinId = firstCoin.get("id").asText();
                        // Fetch and save coin details
                        coinService.getCoinDetails(foundCoinId);
                        coin = coinService.findById(foundCoinId);
                        coinId = foundCoinId;
                    }
                } catch (Exception e) {
                    logger.warn("Could not search for coin: {}", detectedCrypto, e);
                }
            }

            if (coin != null) {
                Map<String, Object> cryptoData = new HashMap<>();
                cryptoData.put("id", coin.getId());
                cryptoData.put("name", coin.getName());
                cryptoData.put("symbol", coin.getSymbol().toUpperCase());
                cryptoData.put("currentPrice", coin.getCurrentPrice());
                cryptoData.put("marketCap", coin.getMarketCap());
                cryptoData.put("marketCapRank", coin.getMarketCapRank());
                cryptoData.put("totalVolume24h", coin.getTotalVolume());
                cryptoData.put("priceChange24h", coin.getPriceChange24h());
                cryptoData.put("priceChangePercentage24h", coin.getPriceChangePercentage24h());
                cryptoData.put("high24h", coin.getHigh24h());
                cryptoData.put("low24h", coin.getLow24h());
                
                data.put("cryptocurrency", cryptoData);
                data.put("coinId", coinId);

                // Get user's holdings for this coin
                try {
                    Asset userAsset = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());
                    if (userAsset != null) {
                        Map<String, Object> holdings = new HashMap<>();
                        holdings.put("quantity", userAsset.getQuantity());
                        holdings.put("buyPrice", userAsset.getBuyPrice());
                        double currentValue = userAsset.getQuantity() * coin.getCurrentPrice();
                        holdings.put("currentValue", currentValue);
                        double profitLoss = currentValue - (userAsset.getQuantity() * userAsset.getBuyPrice());
                        holdings.put("profitLoss", profitLoss);
                        holdings.put("profitLossPercentage", userAsset.getBuyPrice() > 0 
                            ? ((coin.getCurrentPrice() - userAsset.getBuyPrice()) / userAsset.getBuyPrice()) * 100 
                            : 0);
                        data.put("userHoldings", holdings);
                    } else {
                        data.put("userHoldings", null);
                    }
                } catch (Exception e) {
                    logger.debug("Could not fetch user holdings for coin: {}", coinId, e);
                    data.put("userHoldings", null);
                }
            } else {
                data.put("cryptocurrency", null);
                data.put("error", "Cryptocurrency not found: " + (detectedCrypto != null ? detectedCrypto : coinId));
            }
        } catch (Exception e) {
            logger.error("Error resolving cryptocurrency data", e);
            data.put("cryptocurrency", null);
            data.put("error", "Unable to fetch cryptocurrency data: " + e.getMessage());
        }
    }

    /**
     * Resolves asset/portfolio-related data
     */
    private void resolveAssetData(Map<String, Object> data, User user, String coinId) {
        List<Asset> assets = assetService.getUserAssets(user.getId());
        
        // If specific coin is requested, filter assets
        if (coinId != null) {
            assets = assets.stream()
                .filter(asset -> asset.getCoin() != null && coinId.equals(asset.getCoin().getId()))
                .toList();
        }
        
        data.put("assets", assets.stream().map(asset -> {
            Map<String, Object> assetMap = new HashMap<>();
            assetMap.put("coinName", asset.getCoin() != null ? asset.getCoin().getName() : "Unknown");
            assetMap.put("coinSymbol", asset.getCoin() != null ? asset.getCoin().getSymbol() : "N/A");
            assetMap.put("quantity", asset.getQuantity());
            assetMap.put("coinId", asset.getCoin() != null ? asset.getCoin().getId() : "N/A");
            if (asset.getCoin() != null) {
                try {
                    Coin coin = coinService.findById(asset.getCoin().getId());
                    assetMap.put("currentPrice", coin.getCurrentPrice());
                    assetMap.put("currentValue", asset.getQuantity() * coin.getCurrentPrice());
                } catch (Exception e) {
                    logger.debug("Could not fetch current price for asset coin: {}", asset.getCoin().getId());
                }
            }
            return assetMap;
        }).toList());
        data.put("totalAssets", assets.size());
    }

    /**
     * Resolves order-related data
     */
    private void resolveOrderData(Map<String, Object> data, User user, String coinId) {
        List<Order> orders = orderService.getAllOrderUser(user.getId(), null, null);
        
        // If specific coin is requested, filter orders (assuming Order has coin reference)
        // Note: This may need adjustment based on your Order model structure
        if (coinId != null) {
            orders = orders.stream()
                .filter(order -> {
                    // Adjust this based on your Order model structure
                    // For now, we'll include all orders if coin filtering is not directly supported
                    return true;
                })
                .toList();
        }
        
        data.put("orders", orders.stream().map(order -> Map.of(
                "orderId", order.getId(),
                "orderType", order.getOrderType() != null ? order.getOrderType().toString() : "N/A",
                "price", order.getPrice() != null ? order.getPrice().toString() : "0",
                "status", order.getStatus() != null ? order.getStatus().toString() : "N/A",
                "timestamp", order.getTimestamp() != null ? order.getTimestamp().toString() : "N/A"
        )).toList());
        data.put("totalOrders", orders.size());
    }

    /**
     * Resolves trade/market-related data
     */
    private void resolveTradeData(Map<String, Object> data, User user, String coinId) {
        // If specific coin is requested, cryptocurrency data should already be resolved
        if (coinId == null) {
            // Fallback to user's first asset if no coin specified
            List<Asset> assets = assetService.getUserAssets(user.getId());
            if (!assets.isEmpty()) {
                Asset firstAsset = assets.get(0);
                if (firstAsset.getCoin() != null) {
                    try {
                        Coin coin = coinService.findById(firstAsset.getCoin().getId());
                        Map<String, Object> sampleCoin = new HashMap<>();
                        sampleCoin.put("name", coin.getName());
                        sampleCoin.put("symbol", coin.getSymbol());
                        sampleCoin.put("currentPrice", coin.getCurrentPrice());
                        data.put("sampleCoin", sampleCoin);
                    } catch (Exception e) {
                        logger.debug("Could not fetch sample coin data", e);
                    }
                }
            }
        }
    }

    /**
     * Resolves general platform data
     */
    private void resolveGeneralData(Map<String, Object> data, User user) {
        Wallet wallet = walletService.getUserWallet(user);
        List<Asset> assets = assetService.getUserAssets(user.getId());
        List<Order> orders = orderService.getAllOrderUser(user.getId(), null, null);

        data.put("walletBalance", wallet.getBalance() != null ? wallet.getBalance().toString() : "0");
        data.put("totalAssets", assets.size());
        data.put("totalOrders", orders.size());
    }
}

