package com.jeevan.TradingApp.chatbot.service;

import com.jeevan.TradingApp.chatbot.domain.ChatIntent;
import com.jeevan.TradingApp.chatbot.dto.IntentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for detecting user intent from chat messages using keyword matching
 * Also detects cryptocurrency symbols and names mentioned in the message
 */
@Service
public class IntentService {

    private static final Logger logger = LoggerFactory.getLogger(IntentService.class);

    // Common cryptocurrency mappings: symbol/name -> CoinGecko coin ID
    private static final Map<String, String> CRYPTO_MAP = new HashMap<>();
    
    static {
        // Major cryptocurrencies
        CRYPTO_MAP.put("btc", "bitcoin");
        CRYPTO_MAP.put("bitcoin", "bitcoin");
        CRYPTO_MAP.put("eth", "ethereum");
        CRYPTO_MAP.put("ethereum", "ethereum");
        CRYPTO_MAP.put("bnb", "binancecoin");
        CRYPTO_MAP.put("binance coin", "binancecoin");
        CRYPTO_MAP.put("sol", "solana");
        CRYPTO_MAP.put("solana", "solana");
        CRYPTO_MAP.put("ada", "cardano");
        CRYPTO_MAP.put("cardano", "cardano");
        CRYPTO_MAP.put("xrp", "ripple");
        CRYPTO_MAP.put("ripple", "ripple");
        CRYPTO_MAP.put("dot", "polkadot");
        CRYPTO_MAP.put("polkadot", "polkadot");
        CRYPTO_MAP.put("doge", "dogecoin");
        CRYPTO_MAP.put("dogecoin", "dogecoin");
        CRYPTO_MAP.put("matic", "matic-network");
        CRYPTO_MAP.put("polygon", "matic-network");
        CRYPTO_MAP.put("avax", "avalanche-2");
        CRYPTO_MAP.put("avalanche", "avalanche-2");
        CRYPTO_MAP.put("link", "chainlink");
        CRYPTO_MAP.put("chainlink", "chainlink");
        CRYPTO_MAP.put("ltc", "litecoin");
        CRYPTO_MAP.put("litecoin", "litecoin");
        CRYPTO_MAP.put("trx", "tron");
        CRYPTO_MAP.put("tron", "tron");
        CRYPTO_MAP.put("uni", "uniswap");
        CRYPTO_MAP.put("uniswap", "uniswap");
        CRYPTO_MAP.put("atom", "cosmos");
        CRYPTO_MAP.put("cosmos", "cosmos");
        CRYPTO_MAP.put("etc", "ethereum-classic");
        CRYPTO_MAP.put("ethereum classic", "ethereum-classic");
        CRYPTO_MAP.put("xlm", "stellar");
        CRYPTO_MAP.put("stellar", "stellar");
        CRYPTO_MAP.put("algo", "algorand");
        CRYPTO_MAP.put("algorand", "algorand");
        CRYPTO_MAP.put("vet", "vechain");
        CRYPTO_MAP.put("vechain", "vechain");
        CRYPTO_MAP.put("icp", "internet-computer");
        CRYPTO_MAP.put("internet computer", "internet-computer");
        CRYPTO_MAP.put("fil", "filecoin");
        CRYPTO_MAP.put("filecoin", "filecoin");
        CRYPTO_MAP.put("hbar", "hedera-hashgraph");
        CRYPTO_MAP.put("hedera", "hedera-hashgraph");
        CRYPTO_MAP.put("xtz", "tezos");
        CRYPTO_MAP.put("tezos", "tezos");
    }

    // Pattern to match cryptocurrency symbols (e.g., BTC, ETH, $BTC)
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
        "\\b([A-Z]{2,10})\\b|\\$([A-Z]{2,10})\\b",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Detects the intent and cryptocurrency from user's message
     *
     * @param message user's input message
     * @return IntentResult containing intent and detected cryptocurrency
     */
    public IntentResult detectIntentAndCrypto(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new IntentResult(ChatIntent.GENERAL_QUERY, null, null);
        }

        String lowerMessage = message.toLowerCase(Locale.ENGLISH);
        
        // Detect cryptocurrency
        String detectedCrypto = detectCryptocurrency(message, lowerMessage);
        String coinId = detectedCrypto != null ? normalizeCoinId(detectedCrypto) : null;

        // Detect intent
        ChatIntent intent = detectIntentType(lowerMessage, detectedCrypto);

        logger.debug("Detected intent: {}, cryptocurrency: {}, coinId: {}", intent, detectedCrypto, coinId);
        
        return new IntentResult(intent, detectedCrypto, coinId);
    }

    /**
     * Detects the intent type of the user's message based on keywords
     * This method is kept for backward compatibility
     *
     * @param message user's input message
     * @return detected ChatIntent
     */
    public ChatIntent detectIntent(String message) {
        IntentResult result = detectIntentAndCrypto(message);
        return result.getIntent();
    }

    /**
     * Detects cryptocurrency mentioned in the message
     *
     * @param originalMessage original message (case-preserved)
     * @param lowerMessage lowercase message for matching
     * @return detected cryptocurrency symbol or name, null if not found
     */
    private String detectCryptocurrency(String originalMessage, String lowerMessage) {
        // First, try to find exact matches in crypto map
        for (Map.Entry<String, String> entry : CRYPTO_MAP.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (lowerMessage.contains(key)) {
                return entry.getKey(); // Return original case if possible
            }
        }

        // Try pattern matching for symbols (BTC, ETH, etc.)
        Matcher matcher = SYMBOL_PATTERN.matcher(originalMessage);
        while (matcher.find()) {
            String symbol = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (symbol != null) {
                String symbolLower = symbol.toLowerCase();
                // Check if it's a known crypto symbol
                for (Map.Entry<String, String> entry : CRYPTO_MAP.entrySet()) {
                    if (entry.getKey().equals(symbolLower)) {
                        return symbol.toUpperCase();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Normalizes cryptocurrency name/symbol to CoinGecko coin ID
     *
     * @param crypto detected cryptocurrency name or symbol
     * @return normalized coin ID, or null if not found
     */
    private String normalizeCoinId(String crypto) {
        if (crypto == null) {
            return null;
        }
        String key = crypto.toLowerCase();
        return CRYPTO_MAP.getOrDefault(key, null);
    }

    /**
     * Detects intent type considering cryptocurrency mentions
     *
     * @param lowerMessage lowercase message
     * @param detectedCrypto detected cryptocurrency
     * @return ChatIntent
     */
    private ChatIntent detectIntentType(String lowerMessage, String detectedCrypto) {
        // Price-related queries (with or without crypto mention)
        if (containsAny(lowerMessage, "price", "current price", "market price", "how much", "cost", "worth", "value")) {
            if (detectedCrypto != null || containsAny(lowerMessage, "btc", "eth", "bitcoin", "ethereum", "crypto", "coin")) {
                return ChatIntent.TRADE_QUERY;
            }
        }

        // Market data queries
        if (containsAny(lowerMessage, "market cap", "marketcap", "volume", "trading volume", "24h", "24 hour", "market data")) {
            return ChatIntent.TRADE_QUERY;
        }

        // Wallet-related keywords
        if (containsAny(lowerMessage, "wallet", "balance", "money", "fund", "deposit", "withdraw", "transfer")) {
            return ChatIntent.WALLET_QUERY;
        }

        // Asset-related keywords (holdings, portfolio)
        if (containsAny(lowerMessage, "asset", "portfolio", "holding", "holdings", "owned", "my coins", "my crypto", "i have", "i own")) {
            return ChatIntent.ASSET_QUERY;
        }

        // Order-related keywords
        if (containsAny(lowerMessage, "order", "orders", "trade history", "transaction", "buy order", "sell order", "pending order", "order history")) {
            return ChatIntent.ORDER_QUERY;
        }

        // Trade-related keywords
        if (containsAny(lowerMessage, "trade", "trading", "buy", "sell", "purchase")) {
            return ChatIntent.TRADE_QUERY;
        }

        // General crypto queries (trends, predictions, general info)
        if (containsAny(lowerMessage, "trend", "trending", "prediction", "forecast", "what about", "tell me about", "information", "info")) {
            if (detectedCrypto != null || containsAny(lowerMessage, "crypto", "cryptocurrency", "bitcoin", "ethereum")) {
                return ChatIntent.GENERAL_QUERY;
            }
        }

        // If crypto is mentioned but no specific intent, default to trade query
        if (detectedCrypto != null) {
            return ChatIntent.TRADE_QUERY;
        }

        // Default to general query
        return ChatIntent.GENERAL_QUERY;
    }

    /**
     * Helper method to check if message contains any of the given keywords
     */
    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

