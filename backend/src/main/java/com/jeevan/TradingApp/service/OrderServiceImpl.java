package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.OrderStatus;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.request.CreateOrderRequest;
import com.jeevan.TradingApp.modal.*;
import com.jeevan.TradingApp.repository.OrderItemRepository;
import com.jeevan.TradingApp.repository.OrderRepository;
import com.jeevan.TradingApp.exception.OrderValidationException;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AssetService assetService;

    @Override
    public Order createOrder(User user, OrderItem orderItem, OrderType orderType) {
        double price = orderItem.getCoin().getCurrentPrice() * orderItem.getQuantity();
        Order order = new Order();
        order.setUser(user);
        order.setOrderItem(orderItem);
        order.setOrderType(orderType);
        order.setPrice(BigDecimal.valueOf(price));
        order.setTimestamp(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);
        order.setRemainingQuantity(orderItem.getQuantity());
        order.setFilledQuantity(0.0);
        return orderRepository.save(order);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with id " + orderId));
    }

    @Override
    public List<Order> getAllOrderUser(Long userId, OrderType orderType, String assetSymbol) {
        return orderRepository.findByUserId(userId);
    }

    private OrderItem createOrderItem(Coin coin, double quantity, double buyPrice, double sellPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setCoin(coin);
        orderItem.setQuantity(quantity);
        orderItem.setBuyPrice(buyPrice);
        orderItem.setSellPrice(sellPrice);
        return orderItemRepository.save(orderItem);
    }

    @Autowired
    private RiskValidationService riskValidationService;

    @Transactional
    public Order buyAsset(Coin coin, double quantity, User user) {
        if (quantity <= 0) {
            throw new OrderValidationException("Trade quantity must be greater than zero");
        }

        double buyPrice = coin.getCurrentPrice();

        // --- ADDED VALIDATION ---
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCoinId(coin.getId());
        request.setQuantity(quantity);
        request.setOrderType(OrderType.BUY);
        riskValidationService.validateTrade(user, request, buyPrice);
        // -----------------------

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, 0);
        Order order = createOrder(user, orderItem, OrderType.BUY);
        orderItem.setOrder(order);

        order.setStatus(OrderStatus.VALIDATED);

        // This will throw exception if funds insufficient and create TRADE_LOCK
        walletService.payOrderPayment(order, user);
        order.setStatus(OrderStatus.OPEN);

        // Simulating immediate execution for now
        order.setFilledQuantity(quantity);
        order.setRemainingQuantity(0.0);
        order.setStatus(OrderStatus.FILLED);

        // Deduct Fee
        feeService.deductFee(user, order.getPrice(), String.valueOf(order.getId()), "Fee for BUY order");

        // Release lock & Debit actual funds
        System.out.println(
                "OrderServiceImpl: Wallet balance before debit: " + walletService.getUserWallet(user).getBalance());

        walletService.releaseLock(user, order.getPrice(), String.valueOf(order.getId()), "Release lock for execution");
        walletService.debit(user, order.getPrice(), String.valueOf(order.getId()), "Execute BUY order");

        System.out.println(
                "OrderServiceImpl: Wallet balance after debit: " + walletService.getUserWallet(user).getBalance());

        Order saveOrder = orderRepository.save(order);

        Asset oldAsset = assetService.findAssetByUserIdAndCoinId(order.getUser().getId(),
                order.getOrderItem().getCoin().getId());
        if (oldAsset == null) {
            assetService.createAsset(user, orderItem.getCoin(), orderItem.getQuantity());
        } else {
            assetService.updateAsset(oldAsset.getId(), quantity);
        }
        return saveOrder;
    }

    @Transactional
    public Order sellAsset(Coin coin, double quantity, User user) {
        if (quantity <= 0) {
            throw new OrderValidationException("Trade quantity must be greater than zero");
        }

        Asset assetToSell = assetService.findAssetByUserIdAndCoinId(user.getId(), coin.getId());
        if (assetToSell == null) {
            throw new ResourceNotFoundException("Asset not found for this user and coin");
        }

        if (assetToSell.getQuantity() < quantity) {
            throw new OrderValidationException("Insufficient asset quantity to sell");
        }

        double buyPrice = assetToSell.getBuyPrice();
        double sellPrice = coin.getCurrentPrice();

        // --- ADDED VALIDATION ---
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCoinId(coin.getId());
        request.setQuantity(quantity);
        request.setOrderType(OrderType.SELL);
        riskValidationService.validateTrade(user, request, sellPrice);
        // -----------------------

        OrderItem orderItem = createOrderItem(coin, quantity, buyPrice, sellPrice);
        Order order = createOrder(user, orderItem, OrderType.SELL);
        orderItem.setOrder(order);

        order.setStatus(OrderStatus.VALIDATED);
        order.setStatus(OrderStatus.OPEN);

        // Immediate Execution simulation
        order.setFilledQuantity(quantity);
        order.setRemainingQuantity(0.0);
        order.setStatus(OrderStatus.FILLED);

        // Credit funds to wallet
        System.out.println(
                "OrderServiceImpl: Wallet balance before credit: " + walletService.getUserWallet(user).getBalance());

        walletService.credit(user, order.getPrice(), String.valueOf(order.getId()), "Execute SELL order");

        System.out.println(
                "OrderServiceImpl: Wallet balance after credit: " + walletService.getUserWallet(user).getBalance());

        // Deduct Fee
        feeService.deductFee(user, order.getPrice(), String.valueOf(order.getId()), "Fee for SELL order");

        Order savedOrder = orderRepository.save(order);

        Asset updatedAsset = assetService.updateAsset(assetToSell.getId(), -quantity);

        if (updatedAsset.getQuantity() * coin.getCurrentPrice() <= 1) {
            assetService.deleteAsset(updatedAsset.getId());
        }

        return savedOrder;
    }

    @Override
    @Transactional
    public Order processOrder(Coin coin, double quantity, OrderType orderType, User user) {
        if (orderType.equals(OrderType.BUY)) {
            return buyAsset(coin, quantity, user);
        } else if (orderType.equals(OrderType.SELL)) {
            return sellAsset(coin, quantity, user);
        }
        throw new OrderValidationException("Invalid order type: " + orderType);
    }

    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = getOrderById(orderId);

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to cancel this order");
        }

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REJECTED) {
            throw new OrderValidationException("Order cannot be cancelled in current state: " + order.getStatus());
        }

        if (order.getOrderType() == OrderType.BUY && order.getStatus() == OrderStatus.OPEN) {
            // Need to release locked funds
            BigDecimal unexecutedAmount = order.getPrice()
                    .multiply(BigDecimal.valueOf(order.getRemainingQuantity() / order.getOrderItem().getQuantity()));

            walletService.releaseLock(user, unexecutedAmount, String.valueOf(order.getId()),
                    "Release lock due to cancellation");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
