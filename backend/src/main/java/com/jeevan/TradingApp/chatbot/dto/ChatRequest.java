package com.jeevan.TradingApp.chatbot.dto;

import lombok.Data;

/**
 * DTO for incoming chat requests from frontend
 */
@Data
public class ChatRequest {
    private String message;
}


