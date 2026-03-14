package com.jeevan.TradingApp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeevan.TradingApp.domain.OrderStatus;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.Asset;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.OrderRepository;
import com.jeevan.TradingApp.exception.OrderValidationException;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderBookServiceImpl implements OrderBookService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebSocketService webSocketService;

    @Override
    @Transactional
    public Order placeOrder(User user, String coinId, double quantity, BigDecimal price, OrderType orderType) {
        Coin coin = coinService.findById(coinId);

        Order order = new Order();
        order.setUser(user);
        order.setCoin(coin);
        order.setOrderType(orderType);
        order.setPrice(price);
        order.setQuantity(quantity);
        order.setRemainingQuantity(quantity);
        order.setFilledQuantity(0.0);
        order.setStatus(OrderStatus.OPEN);
        order.setTimestamp(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        if (orderType == OrderType.BUY) {
            walletService.payOrderPayment(savedOrder, user);
        } else {
            Asset asset = assetService.findAssetByUserIdAndCoinId(user.getId(), coinId);
            if (asset == null || asset.getQuantity() < quantity) {
                throw new OrderValidationException("Insufficient asset quantity to place this sell order");
            }
            // Temporarily deduct asset. Realistically we might want an "asset_ledger" too,
            // but for now we reduce asset.
            assetService.updateAsset(asset.getId(), -quantity);
        }

        matchOrders(coinId);

        // Push updated order book to all subscribers of this coin
        try {
            webSocketService.pushOrderBookUpdate(coinId, getOrderBook(coinId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedOrder;
    }

    @Override
    @Transactional
    public void matchOrders(String coinId) {
        List<OrderStatus> matchableStatuses = Arrays.asList(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);

        // Fetch buy orders and try to match with sell orders
        List<Order> buyOrders = orderRepository.getMatchingBuyOrders(coinId, BigDecimal.ZERO, OrderType.BUY,
                matchableStatuses);

        for (Order buyOrder : buyOrders) {
            if (buyOrder.getRemainingQuantity() <= 0)
                continue;

            List<Order> matchingSellOrders = orderRepository.getMatchingSellOrders(coinId, buyOrder.getPrice(),
                    OrderType.SELL, matchableStatuses);

            for (Order sellOrder : matchingSellOrders) {
                if (buyOrder.getRemainingQuantity() <= 0)
                    break;
                if (sellOrder.getRemainingQuantity() <= 0)
                    continue;

                double tradeQuantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());

                // update quantites
                buyOrder.setFilledQuantity(buyOrder.getFilledQuantity() + tradeQuantity);
                buyOrder.setRemainingQuantity(buyOrder.getRemainingQuantity() - tradeQuantity);
                sellOrder.setFilledQuantity(sellOrder.getFilledQuantity() + tradeQuantity);
                sellOrder.setRemainingQuantity(sellOrder.getRemainingQuantity() - tradeQuantity);

                updateStatus(buyOrder);
                updateStatus(sellOrder);

                orderRepository.save(buyOrder);
                orderRepository.save(sellOrder);

                // Settlements
                settleTrade(buyOrder, sellOrder, tradeQuantity);

                // Broadcast updates via Redis pub/sub
                broadcastOrderUpdate(buyOrder);
                broadcastOrderUpdate(sellOrder);

                // Push real-time WebSocket updates to each user
                webSocketService.pushUserOrderUpdate(buyOrder.getUser().getId(), buyOrder);
                webSocketService.pushUserOrderUpdate(sellOrder.getUser().getId(), sellOrder);
            }
        }
    }

    private void updateStatus(Order order) {
        if (order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("Price must be greater than zero");
        } else if (order.getFilledQuantity() > 0) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }

    private void settleTrade(Order buyOrder, Order sellOrder, double tradeQuantity) {
        // Assume trade happens at sell_order's price (maker price)
        BigDecimal tradePrice = sellOrder.getPrice();
        BigDecimal totalTradeValue = tradePrice.multiply(BigDecimal.valueOf(tradeQuantity));

        // BUYER: Release lock and Debit actual funds
        walletService.releaseLock(buyOrder.getUser(), totalTradeValue,
                buyOrder.getId().toString(), "Release lock execution");
        walletService.debit(buyOrder.getUser(), totalTradeValue,
                buyOrder.getId().toString(), "Execute BUY order");
        assetService.createAsset(buyOrder.getUser(), buyOrder.getCoin(), tradeQuantity);
        feeService.deductFee(buyOrder.getUser(), totalTradeValue, buyOrder.getId().toString(),
                "Fee for BUY order match");

        // SELLER: Credit funds (Assets already deducted at placement)
        walletService.credit(sellOrder.getUser(), totalTradeValue,
                sellOrder.getId().toString(), "Execute SELL order");
        feeService.deductFee(sellOrder.getUser(), totalTradeValue, sellOrder.getId().toString(),
                "Fee for SELL order match");
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
        if (!order.getUser().getId().equals(user.getId()))
            throw new UnauthorizedAccessException("You are not authorized to modify this order");

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderValidationException("Order cannot be modified in state " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (order.getOrderType() == OrderType.BUY) {
            BigDecimal unexecutedAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getRemainingQuantity()));
            walletService.releaseLock(user, unexecutedAmount,
                    order.getId().toString(), "Release lock cancellation");
        } else {
            // Re-credit the remaining asset
            assetService.createAsset(user, order.getCoin(), order.getRemainingQuantity());
        }

        return order;
    }

    @Override
    public Map<String, List<Order>> getOrderBook(String coinId) {
        List<Order> buys = orderRepository.getMatchingBuyOrders(coinId, BigDecimal.ZERO, OrderType.BUY,
                Arrays.asList(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED));
        List<Order> sells = orderRepository.getMatchingSellOrders(coinId, BigDecimal.valueOf(Long.MAX_VALUE),
                OrderType.SELL, Arrays.asList(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED));

        Map<String, List<Order>> orderbook = new HashMap<>();
        orderbook.put("buyOrders", buys);
        orderbook.put("sellOrders", sells);
        return orderbook;
    }

    private void broadcastOrderUpdate(Order order) {
        try {
            String message = objectMapper.writeValueAsString(order);
            redisTemplate.convertAndSend("order_updates", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
