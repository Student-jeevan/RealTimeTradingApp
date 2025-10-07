package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.OrderItem;
import com.jeevan.TradingApp.modal.User;

import java.util.List;

public interface OrderService {
    Order createOrder(User user , OrderItem orderItem , OrderType orderType);
    Order getOrderById(Long orderId) throws Exception;
    List<Order> getAllOrderUser(Long userId , OrderType orderType , String assetSymbol);
    Order processOrder(Coin coin , double quantity , OrderType orderType , User user) throws Exception;

}
