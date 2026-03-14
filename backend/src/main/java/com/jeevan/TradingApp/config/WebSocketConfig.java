package com.jeevan.TradingApp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register the STOMP endpoint at /ws.
     * SockJS fallback is enabled for browsers that don't support native WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        // SockJS fallback is intentionally omitted — the React frontend
        // uses @stomp/stompjs with a native WebSocket URL (no sockjs-client)
    }

    /**
     * Configure the message broker:
     * - Simple in-memory broker handles /topic (broadcast) and /user
     * (point-to-point) destinations.
     * - Messages sent from the client with prefix /app are routed
     * to @MessageMapping methods.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
