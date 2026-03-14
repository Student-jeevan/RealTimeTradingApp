package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Order;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

public interface OrderBookService {
    Order placeOrder(User user, String coinId, double quantity, java.math.BigDecimal price,
            com.jeevan.TradingApp.domain.OrderType orderType);

    void matchOrders(String coinId);

    Order cancelOrder(Long orderId, User user);

    // For fetching Orderbook responses
    java.util.Map<String, java.util.List<Order>> getOrderBook(String coinId);
}
