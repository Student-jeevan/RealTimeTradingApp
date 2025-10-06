package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.OrderItem;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletService walletService;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        return null;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return null;
    }

    @Override
    public List<Order> getAllOrderUser(Long userId, OrderType orderType, String assetSymbol) {
        return List.of();
    }

    @Override
    public Order processOrder(Coin coin, double quantity, OrderType orderType, User user) {
        return null;
    }
}
