package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for pushing real-time updates over WebSocket (STOMP).
 *
 * Broadcast destinations:
 * /topic/orderbook/{coinId} — updated order book for a coin (all subscribers)
 *
 * User-specific destinations:
 * /user/{userId}/queue/orders — a user's own order status update
 */
@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Push the full order book snapshot to all subscribers of a coin.
     *
     * @param coinId the coin identifier (e.g. "bitcoin")
     * @param book   map with keys "buyOrders" and "sellOrders"
     */
    public void pushOrderBookUpdate(String coinId, Map<String, List<Order>> book) {
        messagingTemplate.convertAndSend("/topic/orderbook/" + coinId, book);
    }

    /**
     * Push an order-status update to the specific user who owns the order.
     * Spring's user-destination mechanism routes this to the correct session(s).
     *
     * @param userId the user's database ID (used as the STOMP "user" principal)
     * @param order  the updated order object
     */
    public void pushUserOrderUpdate(Long userId, Order order) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/orders",
                order);
    }
}
