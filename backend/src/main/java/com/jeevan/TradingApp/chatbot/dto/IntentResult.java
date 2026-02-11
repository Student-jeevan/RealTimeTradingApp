package com.jeevan.TradingApp.chatbot.dto;

import com.jeevan.TradingApp.chatbot.domain.ChatIntent;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO for intent detection result containing both intent type and detected cryptocurrency
 */
@Data
@AllArgsConstructor
public class IntentResult {
    private ChatIntent intent;
    private String detectedCryptocurrency; // Coin ID, symbol, or name (e.g., "bitcoin", "BTC", "Bitcoin")
    private String coinId; // Standardized coin ID (e.g., "bitcoin", "ethereum")
}



