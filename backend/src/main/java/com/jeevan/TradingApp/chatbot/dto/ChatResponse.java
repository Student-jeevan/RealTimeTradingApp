package com.jeevan.TradingApp.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for chatbot responses to frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response;
}

