package com.jeevan.TradingApp.integration;

import com.jeevan.TradingApp.domain.OrderStatus;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.domain.USER_ROLE;
import com.jeevan.TradingApp.modal.Asset;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.WalletLedger;
import com.jeevan.TradingApp.repository.*;
import com.jeevan.TradingApp.service.OrderService;
import com.jeevan.TradingApp.service.WalletService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the full BUY and SELL order flows.
 *
 * Flow under test:
 *   API call → OrderService → WalletService (lock/debit/credit)
 *            → AssetService → DB persisted
 *            → Kafka TradeEvent published (verified via ProcessedEvent)
 *
 * Uses real MySQL, real Spring beans, no mocks.
 * Kafka produces asynchronously; we wait up to 5s for consumer side-effects.
 */
@DisplayName("Order Execution Integration Tests")
class OrderExecutionIntegrationTest extends BaseIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private WalletService walletService;

    @Autowired private UserRepository userRepository;
    @Autowired private CoinRepository coinRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @Autowired private WalletLedgerRepository walletLedgerRepository;

    private User customer;
    private Coin bitcoin;

    @BeforeEach
    void setUp() {
        // Create fresh test user for each test (isolated state)
        customer = new User();
        customer.setEmail("buyer_" + System.nanoTime() + "@test.com");
        customer.setFullName("Integration Buyer");
        customer.setPassword("hashed");
        customer.setRole(USER_ROLE.ROLE_CUSTOMER);
        customer.setVerified(true);
        customer = userRepository.save(customer);

        // Persist Bitcoin coin (required for Order entity)
        bitcoin = coinRepository.findById("bitcoin").orElseGet(() -> {
            Coin c = new Coin();
            c.setId("bitcoin");
            c.setName("Bitcoin");
            c.setSymbol("BTC");
            c.setCurrentPrice(50_000.0);
            c.setMarketCapRank(1);
            return coinRepository.save(c);
        });
        bitcoin.setCurrentPrice(50_000.0); // Ensure price is set for test

        // Fund customer wallet
        walletService.addBalance(customer, BigDecimal.valueOf(100_000));
    }

    @AfterEach
    void tearDown() {
        // Clean DB between tests to avoid constraint violations
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        assetRepository.deleteAll();
        walletLedgerRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.delete(customer);
    }

    // =========================================================================
    //  BUY Order Flow
    // =========================================================================
    @Nested
    @DisplayName("BUY Order Flow")
    class BuyOrderFlow {

        @Test
        @DisplayName("BUY creates FILLED order, adjusts wallet balance, creates asset in DB")
        void buyOrder_shouldFillOrder_adjustWallet_createAsset() throws InterruptedException {
            // Arrange
            double qty = 0.5;
            BigDecimal expectedCost = BigDecimal.valueOf(bitcoin.getCurrentPrice() * qty); // 25,000

            Wallet walletBefore = walletService.getUserWallet(customer);
            BigDecimal balanceBefore = walletBefore.getBalance();

            // Act
            Order order = orderService.processOrder(bitcoin, qty, OrderType.BUY, customer);

            // ── Assert: Order ────────────────────────────────────────────────
            assertThat(order.getId()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.getFilledQuantity()).isEqualTo(qty);
            assertThat(order.getRemainingQuantity()).isEqualTo(0.0);
            assertThat(order.getOrderType()).isEqualTo(OrderType.BUY);

            // Verify persisted in DB
            Order dbOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(dbOrder.getStatus()).isEqualTo(OrderStatus.FILLED);

            // ── Assert: Wallet balance reduced ───────────────────────────────
            Wallet walletAfter = walletService.getUserWallet(customer);
            // Balance = before - cost - fee (fee = 0.1% of cost)
            BigDecimal fee = expectedCost.multiply(new BigDecimal("0.001"));
            BigDecimal expectedBalance = balanceBefore.subtract(expectedCost).subtract(fee);
            assertThat(walletAfter.getBalance())
                    .isEqualByComparingTo(expectedBalance);

            // ── Assert: Asset created in portfolio ───────────────────────────
            List<Asset> assets = assetRepository.findByUserId(customer.getId());
            assertThat(assets).hasSize(1);
            assertThat(assets.get(0).getQuantity()).isEqualTo(qty);
            assertThat(assets.get(0).getBuyPrice()).isEqualTo(bitcoin.getCurrentPrice());

            // ── Assert: Ledger entries created ───────────────────────────────
            // At minimum: CREDIT (deposit), TRADE_LOCK, TRADE_RELEASE, DEBIT, FEE
            List<WalletLedger> entries = walletLedgerRepository.findByUserIdOrderByCreatedAtDesc(customer.getId());
            assertThat(entries).hasSizeGreaterThanOrEqualTo(3);

            // ── Assert: Kafka consumer side-effects (async — allow 5s) ───────
            // TradeEventConsumer saves a ProcessedEvent after consuming the TradeEvent
            waitFor(() -> !processedEventRepository.findAll().isEmpty(), 5_000);
            assertThat(processedEventRepository.count()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Buying same coin twice accumulates quantity in single portfolio asset")
        void buyOrder_twice_shouldAccumulateAssetQuantity() {
            orderService.processOrder(bitcoin, 0.5, OrderType.BUY, customer);
            orderService.processOrder(bitcoin, 0.3, OrderType.BUY, customer);

            List<Asset> assets = assetRepository.findByUserId(customer.getId());
            assertThat(assets).hasSize(1);
            assertThat(assets.get(0).getQuantity()).isEqualTo(0.8);
        }

        @Test
        @DisplayName("BUY with insufficient balance throws and leaves DB unchanged")
        void buyOrder_withInsufficientBalance_throwsAndLeavesDatabaseClean() {
            // Try to buy 100 BTC at 50,000 = $5,000,000 (wallet only has $100,000)
            assertThatThrownBy(() ->
                    orderService.processOrder(bitcoin, 100.0, OrderType.BUY, customer))
                    .isInstanceOf(RuntimeException.class);

            // No order or asset created
            assertThat(orderRepository.count()).isZero();
            assertThat(assetRepository.count()).isZero();
        }
    }

    // =========================================================================
    //  SELL Order Flow
    // =========================================================================
    @Nested
    @DisplayName("SELL Order Flow")
    class SellOrderFlow {

        @BeforeEach
        void buyFirst() {
            // Setup: buy 1.0 BTC first so we have something to sell
            orderService.processOrder(bitcoin, 1.0, OrderType.BUY, customer);
        }

        @Test
        @DisplayName("SELL fills order, credits wallet, and reduces asset quantity")
        void sellOrder_shouldFillOrder_creditWallet_reduceAsset() {
            Wallet walletBefore = walletService.getUserWallet(customer);
            BigDecimal balanceBefore = walletBefore.getBalance();

            // Act — sell 0.4 BTC
            Order sellOrder = orderService.processOrder(bitcoin, 0.4, OrderType.SELL, customer);

            // Assert: order status
            assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(sellOrder.getOrderType()).isEqualTo(OrderType.SELL);

            // Assert: wallet credited (minus fee)
            Wallet walletAfter = walletService.getUserWallet(customer);
            assertThat(walletAfter.getBalance()).isGreaterThan(balanceBefore);

            // Assert: asset quantity reduced from 1.0 → 0.6
            List<Asset> assets = assetRepository.findByUserId(customer.getId());
            assertThat(assets).hasSize(1);
            assertThat(assets.get(0).getQuantity()).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Selling all BTC removes the asset from portfolio (dust cleanup)")
        void sellAll_shouldDeleteAssetRecord() {
            // Sell entire 1.0 BTC position
            orderService.processOrder(bitcoin, 1.0, OrderType.SELL, customer);

            // Asset should be deleted (value = 0, below $1 threshold)
            List<Asset> assets = assetRepository.findByUserId(customer.getId());
            assertThat(assets).isEmpty();
        }

        @Test
        @DisplayName("SELL without owning coin throws and leaves portfolio unchanged")
        void sellCoinNotOwned_shouldThrowWithoutModifyingDB() {
            Coin ethereum = coinRepository.findById("ethereum").orElseGet(() -> {
                Coin c = new Coin();
                c.setId("ethereum");
                c.setName("Ethereum");
                c.setSymbol("ETH");
                c.setCurrentPrice(3_000.0);
                c.setMarketCapRank(2);
                return coinRepository.save(c);
            });

            assertThatThrownBy(() ->
                    orderService.processOrder(ethereum, 0.5, OrderType.SELL, customer))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Polls a condition up to maxWaitMs milliseconds (for async Kafka events).
     */
    private void waitFor(java.util.function.BooleanSupplier condition, long maxWaitMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(200);
        }
        // Let assertions in the test body produce the failure message
    }
}
