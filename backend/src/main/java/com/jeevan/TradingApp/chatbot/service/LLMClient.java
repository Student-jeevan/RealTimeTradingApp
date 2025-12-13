package com.jeevan.TradingApp.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Client for communicating with external LLM API
 * API URL and key must be configured in application.properties
 */
@Service
public class LLMClient {

    @Value("${chatbot.api.url:}")
    private String apiUrl;

    @Value("${chatbot.api.key:}")
    private String apiKey;

    @Value("${chatbot.google.service.account.path:}")
    private String serviceAccountPath;

    private final HttpClient httpClient;

    public LLMClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends prompt to LLM API and returns response
     *
     * @param prompt formatted prompt string
     * @return LLM response text
     */
    public String getResponse(String prompt) {
        if ((apiUrl == null || apiUrl.isEmpty()) && (serviceAccountPath == null || serviceAccountPath.isEmpty())) {
            return "AI service is not configured. Please configure chatbot.api.url and chatbot.api.key (or chatbot.google.service.account.path) in application.properties.";
        }

        try {
            String requestBody = buildRequestBody(prompt);
            String authHeader = buildAuthHeader();
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

            if (authHeader != null && !authHeader.isEmpty()) {
                requestBuilder.header("Authorization", authHeader);
            }

            URI uri = buildApiUri();
            HttpRequest request = requestBuilder.uri(uri).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                return "Error: AI service returned status code " + response.statusCode() + ". Response: " + response.body();
            }
        } catch (Exception e) {
            return "Error communicating with AI service: " + e.getMessage();
        }
    }

    /**
     * Builds the API URI based on configuration
     */
    private URI buildApiUri() {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            return URI.create(apiUrl);
        }
        // Default to Google Vertex AI endpoint if using service account
        return URI.create("https://us-central1-aiplatform.googleapis.com/v1/projects/tradingchatbot/locations/us-central1/publishers/google/models/chat-bison:predict");
    }

    /**
     * Builds authorization header based on configuration
     */
    private String buildAuthHeader() {
        if (apiKey != null && !apiKey.isEmpty()) {
            return "Bearer " + apiKey;
        }
        // For Google Cloud service account, OAuth token should be obtained separately
        return null;
    }

    /**
     * Builds request body for LLM API
     * Modify this method based on your LLM provider's API format
     */
    private String buildRequestBody(String prompt) {
        return String.format(
                "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                escapeJson(prompt)
        );
    }

    /**
     * Parses response from LLM API
     * Modify this method based on your LLM provider's response format
     */
    private String parseResponse(String responseBody) {
        try {
            int startIndex = responseBody.indexOf("\"content\":\"") + 11;
            int endIndex = responseBody.indexOf("\"", startIndex);
            if (startIndex > 10 && endIndex > startIndex) {
                String content = responseBody.substring(startIndex, endIndex);
                return unescapeJson(content);
            }
            return "Unable to parse AI response";
        } catch (Exception e) {
            return "Error parsing AI response: " + e.getMessage();
        }
    }

    /**
     * Escapes JSON special characters
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Unescapes JSON special characters
     */
    private String unescapeJson(String str) {
        return str.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}

