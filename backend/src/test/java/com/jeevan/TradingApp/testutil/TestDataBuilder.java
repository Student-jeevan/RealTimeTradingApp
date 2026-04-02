package com.jeevan.TradingApp.testutil;

import com.jeevan.TradingApp.domain.USER_ROLE;
import com.jeevan.TradingApp.domain.WithdrawalStatus;
import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.kafka.events.PriceUpdateEvent;
import com.jeevan.TradingApp.kafka.events.TradeEvent;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.modal.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Central factory for test data builders.
 * Use these helpers in ALL tests to avoid magic literals and keep
 * test data construction consistent across the suite.
 */
public final class TestDataBuilder {

    private TestDataBuilder() { /* utility class */ }

    // ─────────────────────────────────────────────
    //  User
    // ─────────────────────────────────────────────

    /** Creates a standard customer with a wallet-ready email. */
    public static User buildCustomer() {
        return buildCustomer(1L, "customer@trading.com");
    }

    public static User buildCustomer(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName("Test Customer");
        u.setPassword("$2a$10$hashed");
        u.setRole(USER_ROLE.ROLE_CUSTOMER);
        u.setVerified(true);
        return u;
    }

    public static User buildAdmin() {
        User u = new User();
        u.setId(99L);
        u.setEmail("admin@trading.com");
        u.setFullName("System Admin");
        u.setPassword("$2a$10$hashed");
        u.setRole(USER_ROLE.ROLE_ADMIN);
        u.setVerified(true);
        return u;
    }

    // ─────────────────────────────────────────────
    //  Wallet
    // ─────────────────────────────────────────────

    public static Wallet buildWallet(User user, BigDecimal balance) {
        Wallet w = new Wallet();
        w.setId(10L);
        w.setUser(user);
        w.setBalance(balance);
        w.setLockedBalance(BigDecimal.ZERO);
        return w;
    }

    public static Wallet buildWallet(User user, BigDecimal balance, BigDecimal locked) {
        Wallet w = buildWallet(user, balance);
        w.setLockedBalance(locked);
        return w;
    }

    // ─────────────────────────────────────────────
    //  Coin
    // ─────────────────────────────────────────────

    public static Coin buildBitcoin() {
        return buildCoin("bitcoin", "BTC", "Bitcoin", 50_000.0);
    }

    public static Coin buildCoin(String id, String symbol, String name, double price) {
        Coin c = new Coin();
        c.setId(id);
        c.setSymbol(symbol);
        c.setName(name);
        c.setCurrentPrice(price);
        c.setMarketCapRank(1);
        c.setMarketCap(900_000_000_000L);
        c.setTotalVolume(30_000_000_000L);
        c.setPriceChange24h(500.0);
        c.setPriceChangePercentage24h(1.0);
        return c;
    }

    // ─────────────────────────────────────────────
    //  Withdrawal
    // ─────────────────────────────────────────────

    public static Withdrawal buildPendingWithdrawal(User user, Long amount) {
        Withdrawal w = new Withdrawal();
        w.setId(20L);
        w.setUser(user);
        w.setAmount(amount);
        w.setStatus(WithdrawalStatus.PENDING);
        w.setDate(LocalDateTime.now());
        return w;
    }

    // ────────────────────────────────────────────-
    //  Kafka Events
    // ─────────────────────────────────────────────

    public static TradeEvent buildTradeEvent(String orderType) {
        return TradeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .userId(1L)
                .userEmail("customer@trading.com")
                .orderId(100L)
                .orderType(orderType)
                .coinId("bitcoin")
                .coinSymbol("btc")
                .quantity(0.5)
                .price(BigDecimal.valueOf(25_000))
                .status("FILLED")
                .build();
    }

    public static PriceUpdateEvent buildPriceUpdateEvent(String coinId, double price) {
        return PriceUpdateEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .coinId(coinId)
                .coinSymbol(coinId)
                .coinName(coinId.toUpperCase())
                .currentPrice(price)
                .priceChange24h(100.0)
                .priceChangePercentage24h(0.5)
                .marketCap(900_000_000_000L)
                .totalVolume(30_000_000_000L)
                .build();
    }

    public static NotificationEvent buildNotificationEvent(String type) {
        return NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .userId(1L)
                .email("customer@trading.com")
                .type(type)
                .subject("Test Notification: " + type)
                .body("<div>Test body</div>")
                .build();
    }

    public static TransactionEvent buildTransactionEvent(String txType) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .userId(1L)
                .transactionType(txType)
                .amount(BigDecimal.valueOf(500))
                .referenceId("order-100")
                .description("Test transaction")
                .build();
    }

    // ─────────────────────────────────────────────
    //  ProcessedEvent
    // ─────────────────────────────────────────────

    public static ProcessedEvent buildProcessedEvent(String eventId) {
        ProcessedEvent pe = new ProcessedEvent();
        pe.setEventId(eventId);
        return pe;
    }
}
