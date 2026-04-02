package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderBookService {
    Order placeOrder(User user, String coinId, double quantity, BigDecimal price, OrderType orderType);

    void matchOrders(String coinId);

    Order cancelOrder(Long orderId, User user);

    // For fetching Orderbook responses
    Map<String, List<Order>> getOrderBook(String coinId);
}
