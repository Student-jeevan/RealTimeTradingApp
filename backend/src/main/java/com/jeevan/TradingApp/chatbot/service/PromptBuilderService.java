package com.jeevan.TradingApp.chatbot.service;

import com.jeevan.TradingApp.chatbot.domain.ChatIntent;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for building structured prompts with user question and platform data
 */
@Service
public class PromptBuilderService {

    /**
     * Builds a structured prompt combining user question with resolved platform data
     *
     * @param userMessage user's original question
     * @param intent detected intent
     * @param data resolved platform data
     * @return formatted prompt string
     */
    public String buildPrompt(String userMessage, ChatIntent intent, Map<String, Object> data) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a helpful trading platform assistant. Answer the user's question using the provided real-time platform data.\n\n");
        prompt.append("User Question: ").append(userMessage).append("\n\n");

        prompt.append("Context - Detected Intent: ").append(intent.name()).append("\n\n");

        prompt.append("Real-Time Platform Data:\n");
        formatData(prompt, data, 0);

        prompt.append("\nInstructions:\n");
        prompt.append("- Answer directly and concisely using the provided data\n");
        prompt.append("- If data is not available, politely explain that\n");
        prompt.append("- Format numbers clearly (e.g., $1,234.56)\n");
        prompt.append("- Be friendly and professional\n");

        return prompt.toString();
    }

    /**
     * Recursively formats data map into readable string
     */
    private void formatData(StringBuilder sb, Object data, int indent) {
        String indentStr = "  ".repeat(indent);

        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(indentStr).append(entry.getKey()).append(": ");
                if (entry.getValue() instanceof Map || entry.getValue() instanceof java.util.List) {
                    sb.append("\n");
                    formatData(sb, entry.getValue(), indent + 1);
                } else {
                    sb.append(entry.getValue()).append("\n");
                }
            }
        } else if (data instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) data;
            for (int i = 0; i < list.size(); i++) {
                sb.append(indentStr).append("[").append(i).append("]:\n");
                formatData(sb, list.get(i), indent + 1);
            }
        } else {
            sb.append(indentStr).append(data).append("\n");
        }
    }
}

