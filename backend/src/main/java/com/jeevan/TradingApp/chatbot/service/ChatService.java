package com.jeevan.TradingApp.chatbot.service;

import com.jeevan.TradingApp.chatbot.domain.ChatIntent;
import com.jeevan.TradingApp.chatbot.dto.ChatResponse;
import com.jeevan.TradingApp.chatbot.dto.IntentResult;
import com.jeevan.TradingApp.modal.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main service orchestrating the chatbot flow
 * Coordinates intent detection, data resolution, prompt building, and LLM communication
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final IntentService intentService;
    private final DataResolverService dataResolverService;
    private final PromptBuilderService promptBuilderService;
    private final LLMClient llmClient;

    public ChatService(
            IntentService intentService,
            DataResolverService dataResolverService,
            PromptBuilderService promptBuilderService,
            LLMClient llmClient
    ) {
        this.intentService = intentService;
        this.dataResolverService = dataResolverService;
        this.promptBuilderService = promptBuilderService;
        this.llmClient = llmClient;
    }

    /**
     * Processes user chat message and returns response
     *
     * @param userMessage user's input message
     * @param user authenticated user
     * @return ChatResponse with bot's answer
     */
    public ChatResponse processMessage(String userMessage, User user) {
        try {
            logger.info("Processing chat message from user: {}", user.getEmail());

            // Step 1: Detect intent and cryptocurrency
            IntentResult intentResult = intentService.detectIntentAndCrypto(userMessage);
            logger.debug("Detected intent: {}, cryptocurrency: {}, coinId: {}", 
                intentResult.getIntent(), intentResult.getDetectedCryptocurrency(), intentResult.getCoinId());

            // Step 2: Resolve relevant data
            Map<String, Object> data = dataResolverService.resolveData(intentResult, user);
            logger.debug("Resolved data for intent: {}", intentResult.getIntent());

            // Step 3: Get response from LLM or fallback
            String response = llmClient.getResponse(
                promptBuilderService.buildPrompt(userMessage, intentResult.getIntent(), data));
            
            // Step 4: If AI is not configured, use fallback response
            if (response.contains("AI service is not configured")) {
                response = generateFallbackResponse(userMessage, intentResult, data);
            }
            
            logger.info("Generated response for user");

            return new ChatResponse(response);

        } catch (Exception e) {
            logger.error("Error processing chat message", e);
            return new ChatResponse("Sorry, I encountered an error processing your request. Please try again.");
        }
    }

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Generates fallback response when AI service is not configured
     * Uses resolved data to provide intelligent answers
     */
    private String generateFallbackResponse(String userMessage, IntentResult intentResult, Map<String, Object> data) {
        String lowerMessage = userMessage.toLowerCase();
        ChatIntent intent = intentResult.getIntent();
        
        // Check if cryptocurrency data is available
        if (data.containsKey("cryptocurrency") && data.get("cryptocurrency") != null) {
            return generateCryptocurrencyResponse(userMessage, intentResult, data, lowerMessage);
        }
        
        switch (intent) {
            case WALLET_QUERY:
                return generateWalletResponse(data, lowerMessage);
            case ASSET_QUERY:
                return generateAssetResponse(data, lowerMessage);
            case ORDER_QUERY:
                return generateOrderResponse(data, lowerMessage);
            case TRADE_QUERY:
                return generateTradeResponse(data, lowerMessage);
            case GENERAL_QUERY:
            default:
                return generateGeneralResponse(data, lowerMessage);
        }
    }

    /**
     * Generates wallet-related response
     */
    @SuppressWarnings("unchecked")
    private String generateWalletResponse(Map<String, Object> data, String message) {
        if (data.containsKey("wallet")) {
            Map<String, Object> wallet = (Map<String, Object>) data.get("wallet");
            String balance = (String) wallet.get("balance");
            
            if (message.contains("balance") || message.contains("money") || message.contains("fund")) {
                return String.format("Your current wallet balance is $%s. You can add money through the Top Up feature or receive funds from trades.", balance);
            }
            return String.format("Wallet Information:\n- Balance: $%s\n- Wallet ID: %s\n\nYou can add money, withdraw, or transfer funds using the wallet features.", 
                    balance, wallet.get("walletId"));
        }
        return "I couldn't retrieve your wallet information at the moment. Please try again later.";
    }

    /**
     * Generates asset/portfolio-related response
     */
    private String generateAssetResponse(Map<String, Object> data, String message) {
        if (data.containsKey("assets")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assets = (List<Map<String, Object>>) data.get("assets");
            Integer totalAssets = (Integer) data.get("totalAssets");
            
            if (totalAssets == null || totalAssets == 0) {
                return "You currently don't have any assets in your portfolio. You can buy cryptocurrencies through the trading features.";
            }
            
            StringBuilder response = new StringBuilder();
            response.append(String.format("You have %d asset(s) in your portfolio:\n\n", totalAssets));
            
            for (int i = 0; i < Math.min(assets.size(), 5); i++) {
                Map<String, Object> asset = assets.get(i);
                response.append(String.format("- %s (%s): Quantity: %s\n", 
                        asset.get("coinName"), 
                        asset.get("coinSymbol"),
                        asset.get("quantity")));
            }
            
            if (assets.size() > 5) {
                response.append(String.format("\n... and %d more asset(s)", assets.size() - 5));
            }
            
            return response.toString();
        }
        return "I couldn't retrieve your asset information at the moment. Please try again later.";
    }

    /**
     * Generates order-related response
     */
    private String generateOrderResponse(Map<String, Object> data, String message) {
        if (data.containsKey("orders")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) data.get("orders");
            Integer totalOrders = (Integer) data.get("totalOrders");
            
            if (totalOrders == null || totalOrders == 0) {
                return "You don't have any orders yet. You can create buy or sell orders through the trading interface.";
            }
            
            StringBuilder response = new StringBuilder();
            response.append(String.format("You have %d order(s):\n\n", totalOrders));
            
            for (int i = 0; i < Math.min(orders.size(), 5); i++) {
                Map<String, Object> order = orders.get(i);
                response.append(String.format("- Order #%s: Type: %s, Price: $%s, Status: %s\n", 
                        order.get("orderId"),
                        order.get("orderType"),
                        order.get("price"),
                        order.get("status")));
            }
            
            if (orders.size() > 5) {
                response.append(String.format("\n... and %d more order(s)", orders.size() - 5));
            }
            
            return response.toString();
        }
        return "I couldn't retrieve your order information at the moment. Please try again later.";
    }

    /**
     * Generates cryptocurrency-related response
     */
    @SuppressWarnings("unchecked")
    private String generateCryptocurrencyResponse(String userMessage, IntentResult intentResult, 
                                                  Map<String, Object> data, String lowerMessage) {
        Map<String, Object> crypto = (Map<String, Object>) data.get("cryptocurrency");
        if (crypto == null) {
            return "I couldn't find information about that cryptocurrency. Please check the name or symbol and try again.";
        }

        String coinName = (String) crypto.get("name");
        String symbol = (String) crypto.get("symbol");
        Double currentPrice = (Double) crypto.get("currentPrice");
        Long marketCap = ((Number) crypto.get("marketCap")).longValue();
        Long volume24h = ((Number) crypto.get("totalVolume24h")).longValue();
        Double priceChange24h = (Double) crypto.get("priceChange24h");
        Double priceChangePercentage24h = (Double) crypto.get("priceChangePercentage24h");
        Integer marketCapRank = (Integer) crypto.get("marketCapRank");

        StringBuilder response = new StringBuilder();

        // Price queries
        if (lowerMessage.contains("price") || lowerMessage.contains("how much") || 
            lowerMessage.contains("cost") || lowerMessage.contains("worth")) {
            response.append(String.format("Current price of %s (%s): %s\n", 
                coinName, symbol, formatCurrency(currentPrice)));
            
            if (priceChange24h != null && priceChange24h != 0) {
                String changeIndicator = priceChange24h >= 0 ? "↑" : "↓";
                response.append(String.format("24h change: %s%s (%s%%) %s\n",
                    changeIndicator, formatCurrency(Math.abs(priceChange24h)),
                    formatPercentage(priceChangePercentage24h), 
                    priceChange24h >= 0 ? "gain" : "loss"));
            }
        }

        // Market cap queries
        if (lowerMessage.contains("market cap") || lowerMessage.contains("marketcap")) {
            response.append(String.format("%s (%s) Market Cap: %s\n", coinName, symbol, formatLargeNumber(marketCap)));
            if (marketCapRank != null) {
                response.append(String.format("Market Cap Rank: #%d\n", marketCapRank));
            }
        }

        // Volume queries
        if (lowerMessage.contains("volume") || lowerMessage.contains("trading volume")) {
            response.append(String.format("24h Trading Volume: %s\n", formatLargeNumber(volume24h)));
        }

        // User holdings queries
        if (data.containsKey("userHoldings") && data.get("userHoldings") != null) {
            Map<String, Object> holdings = (Map<String, Object>) data.get("userHoldings");
            Double quantity = (Double) holdings.get("quantity");
            Double currentValue = (Double) holdings.get("currentValue");
            Double profitLoss = (Double) holdings.get("profitLoss");
            Double profitLossPercentage = (Double) holdings.get("profitLossPercentage");

            response.append(String.format("\nYour Holdings:\n"));
            response.append(String.format("- Quantity: %s %s\n", formatDecimal(quantity), symbol));
            response.append(String.format("- Current Value: %s\n", formatCurrency(currentValue)));
            
            if (profitLoss != null) {
                String plIndicator = profitLoss >= 0 ? "profit" : "loss";
                response.append(String.format("- %s: %s (%s%%)\n",
                    plIndicator.substring(0, 1).toUpperCase() + plIndicator.substring(1),
                    formatCurrency(Math.abs(profitLoss)),
                    formatPercentage(Math.abs(profitLossPercentage))));
            }
        } else if (lowerMessage.contains("my") || lowerMessage.contains("i have") || 
                   lowerMessage.contains("i own") || lowerMessage.contains("holding")) {
            response.append(String.format("\nYou don't currently own any %s (%s).\n", coinName, symbol));
        }

        // Market data summary
        if (response.length() == 0 || lowerMessage.contains("info") || lowerMessage.contains("information") ||
            lowerMessage.contains("tell me about") || lowerMessage.contains("data")) {
            response.append(String.format("%s (%s) Market Data:\n", coinName, symbol));
            response.append(String.format("- Current Price: %s\n", formatCurrency(currentPrice)));
            response.append(String.format("- Market Cap: %s (Rank #%d)\n", formatLargeNumber(marketCap), marketCapRank));
            response.append(String.format("- 24h Volume: %s\n", formatLargeNumber(volume24h)));
            if (priceChange24h != null) {
                response.append(String.format("- 24h Change: %s%%\n", formatPercentage(priceChangePercentage24h)));
            }
        }

        // Predictions/trends placeholder
        if (lowerMessage.contains("predict") || lowerMessage.contains("forecast") || 
            lowerMessage.contains("trend") || lowerMessage.contains("future")) {
            response.append("\nNote: Prediction and trend analysis features are coming soon.");
        }

        return response.toString();
    }

    /**
     * Generates trade-related response
     */
    private String generateTradeResponse(Map<String, Object> data, String message) {
        // Check for cryptocurrency data first
        if (data.containsKey("cryptocurrency") && data.get("cryptocurrency") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> crypto = (Map<String, Object>) data.get("cryptocurrency");
            String coinName = (String) crypto.get("name");
            String symbol = (String) crypto.get("symbol");
            Double currentPrice = (Double) crypto.get("currentPrice");
            
            return String.format("Trading Information for %s (%s):\n- Current Price: %s\n\nYou can buy or sell %s through the trading interface.", 
                coinName, symbol, formatCurrency(currentPrice), coinName);
        }
        
        if (data.containsKey("sampleCoin")) {
            Object sampleCoin = data.get("sampleCoin");
            if (sampleCoin instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> coin = (Map<String, Object>) sampleCoin;
                return String.format("Trading Information:\n- Sample Coin: %s (%s)\n- Current Price: %s\n\nYou can buy or sell cryptocurrencies through the trading interface.", 
                        coin.get("name"), 
                        coin.get("symbol"),
                        formatCurrency(((Number) coin.get("currentPrice")).doubleValue()));
            }
        }
        return "I can help you with trading information. You can ask about specific coin prices, buy/sell orders, or check your trading history.";
    }

    /**
     * Generates general response
     */
    private String generateGeneralResponse(Map<String, Object> data, String message) {
        StringBuilder response = new StringBuilder();
        response.append("Here's a summary of your account:\n\n");
        
        if (data.containsKey("walletBalance")) {
            response.append(String.format("- Wallet Balance: $%s\n", data.get("walletBalance")));
        }
        if (data.containsKey("totalAssets")) {
            response.append(String.format("- Total Assets: %s\n", data.get("totalAssets")));
        }
        if (data.containsKey("totalOrders")) {
            response.append(String.format("- Total Orders: %s\n", data.get("totalOrders")));
        }
        
        response.append("\nYou can ask me about:\n");
        response.append("- Cryptocurrency prices (e.g., 'What's the price of Bitcoin?')\n");
        response.append("- Market data (e.g., 'Market cap of Ethereum')\n");
        response.append("- Your wallet balance\n");
        response.append("- Your assets and portfolio\n");
        response.append("- Your order history\n");
        response.append("- Trading information\n");
        response.append("\nNote: For AI-powered responses, please configure chatbot.api.url and chatbot.api.key in application.properties.");
        
        return response.toString();
    }

    /**
     * Helper methods for formatting numbers
     */
    private String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    private String formatDecimal(double number) {
        return DECIMAL_FORMAT.format(number);
    }

    private String formatPercentage(double percentage) {
        return PERCENTAGE_FORMAT.format(percentage);
    }

    private String formatLargeNumber(long number) {
        if (number >= 1_000_000_000_000L) {
            return String.format("$%.2fT", number / 1_000_000_000_000.0);
        } else if (number >= 1_000_000_000L) {
            return String.format("$%.2fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000L) {
            return String.format("$%.2fM", number / 1_000_000.0);
        } else if (number >= 1_000L) {
            return String.format("$%.2fK", number / 1_000.0);
        }
        return CURRENCY_FORMAT.format(number);
    }
}

