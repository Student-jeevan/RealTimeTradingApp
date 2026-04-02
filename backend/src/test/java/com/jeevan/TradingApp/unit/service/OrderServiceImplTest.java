package com.jeevan.TradingApp.unit.service;

import com.jeevan.TradingApp.domain.OrderStatus;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.exception.InsufficientBalanceException;
import com.jeevan.TradingApp.exception.OrderValidationException;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import com.jeevan.TradingApp.kafka.producer.TradeEventProducer;
import com.jeevan.TradingApp.modal.*;
import com.jeevan.TradingApp.repository.OrderItemRepository;
import com.jeevan.TradingApp.repository.OrderRepository;
import com.jeevan.TradingApp.service.*;
import com.jeevan.TradingApp.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl covering:
 * - BUY asset: happy path, validation, risk checks, asset creation/update
 * - SELL asset: happy path, missing asset, insufficient quantity, dust cleanup
 * - processOrder: routing to buy/sell
 * - cancelOrder: authorization, state guards, lock release
 * - Kafka event publishing (fire-and-forget)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private WalletService walletService;
    @Mock private FeeService feeService;
    @Mock private AssetService assetService;
    @Mock private TradeEventProducer tradeEventProducer;
    @Mock private RiskValidationService riskValidationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ── Test Data ─────────────────────────────────────────────────────────────
    private User customer;
    private Coin bitcoin;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        customer = TestDataBuilder.buildCustomer();
        bitcoin  = TestDataBuilder.buildBitcoin();   // price = 50,000
        wallet   = TestDataBuilder.buildWallet(customer, BigDecimal.valueOf(100_000));
    }

    // =========================================================================
    //  BUY ASSET
    // =========================================================================
    @Nested
    @DisplayName("buyAsset")
    class BuyAsset {

        @Test
        @DisplayName("Happy path: BUY fills order, creates new asset, publishes Kafka event")
        void shouldFillOrder_createNewAsset_andPublishTradeEvent() {
            // Arrange — no existing asset for this user+coin
            when(walletService.getUserWallet(customer)).thenReturn(wallet);
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
                OrderItem item = inv.getArgument(0);
                item.setId(50L);
                return item;
            });
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                if (o.getId() == null) o.setId(100L);
                return o;
            });
            when(assetService.findAssetByUserIdAndCoinId(customer.getId(), bitcoin.getId()))
                    .thenReturn(null);
            when(walletService.payOrderPayment(any(Order.class), eq(customer))).thenReturn(wallet);
            when(walletService.releaseLock(any(), any(), any(), any())).thenReturn(wallet);
            when(walletService.debit(any(), any(), any(), any())).thenReturn(wallet);
            doNothing().when(riskValidationService).validateTrade(any(), any(), anyDouble());

            // Act
            Order result = orderService.buyAsset(bitcoin, 0.5, customer);

            // Assert — order must be FILLED
            assertThat(result.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(result.getFilledQuantity()).isEqualTo(0.5);
            assertThat(result.getRemainingQuantity()).isEqualTo(0.0);

            // Wallet operations must be performed in order
            verify(walletService).payOrderPayment(any(), eq(customer));   // 1. lock
            verify(walletService).releaseLock(eq(customer), any(), any(), any()); // 2. release
            verify(walletService).debit(eq(customer), any(), any(), any());       // 3. debit

            // New asset must be created (no existing asset)
            verify(assetService).createAsset(eq(customer), eq(bitcoin), eq(0.5));

            // Kafka event must be published
            verify(tradeEventProducer).publish(any());
        }

        @Test
        @DisplayName("BUY updates existing asset quantity instead of creating a new one")
        void shouldUpdateExistingAsset_whenUserAlreadyHoldsCoin() {
            // Arrange — user already has 1.0 BTC
            Asset existingAsset = new Asset();
            existingAsset.setId(77L);
            existingAsset.setQuantity(1.0);
            existingAsset.setBuyPrice(bitcoin.getCurrentPrice());

            when(walletService.getUserWallet(customer)).thenReturn(wallet);
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
                OrderItem item = inv.getArgument(0);
                item.setId(50L);
                return item;
            });
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                if (o.getId() == null) o.setId(100L);
                return o;
            });
            when(assetService.findAssetByUserIdAndCoinId(customer.getId(), bitcoin.getId()))
                    .thenReturn(existingAsset);
            when(walletService.payOrderPayment(any(), eq(customer))).thenReturn(wallet);
            when(walletService.releaseLock(any(), any(), any(), any())).thenReturn(wallet);
            when(walletService.debit(any(), any(), any(), any())).thenReturn(wallet);
            doNothing().when(riskValidationService).validateTrade(any(), any(), anyDouble());

            // Act
            orderService.buyAsset(bitcoin, 0.5, customer);

            // Assert — updateAsset called, NOT createAsset
            verify(assetService).updateAsset(eq(77L), eq(0.5));
            verify(assetService, never()).createAsset(any(), any(), anyDouble());
        }

        @Test
        @DisplayName("Throws OrderValidationException when quantity <= 0")
        void shouldThrow_whenQuantityIsZero() {
            assertThatThrownBy(() -> orderService.buyAsset(bitcoin, 0.0, customer))
                    .isInstanceOf(OrderValidationException.class);

            // No DB writes should happen
            verifyNoInteractions(orderRepository, walletService, tradeEventProducer);
        }

        @Test
        @DisplayName("Throws OrderValidationException when quantity is negative")
        void shouldThrow_whenQuantityIsNegative() {
            assertThatThrownBy(() -> orderService.buyAsset(bitcoin, -1.0, customer))
                    .isInstanceOf(OrderValidationException.class);
        }

        @Test
        @DisplayName("Propagates InsufficientBalanceException from walletService")
        void shouldPropagate_whenWalletHasInsufficientBalance() {
            // Arrange
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new InsufficientBalanceException("Insufficient funds"))
                    .when(walletService).payOrderPayment(any(), eq(customer));
            doNothing().when(riskValidationService).validateTrade(any(), any(), anyDouble());

            // Act & Assert
            assertThatThrownBy(() -> orderService.buyAsset(bitcoin, 10.0, customer))
                    .isInstanceOf(InsufficientBalanceException.class);

            // Kafka event must NOT be published on failed trade
            verify(tradeEventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("Kafka publish failure does NOT fail the buy transaction")
        void tradeEventPublishFailure_shouldNotRollbackBuyOrder() {
            // Arrange — Kafka producer throws
            when(walletService.getUserWallet(customer)).thenReturn(wallet);
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assetService.findAssetByUserIdAndCoinId(any(), any())).thenReturn(null);
            when(walletService.payOrderPayment(any(), any())).thenReturn(wallet);
            when(walletService.releaseLock(any(), any(), any(), any())).thenReturn(wallet);
            when(walletService.debit(any(), any(), any(), any())).thenReturn(wallet);
            doNothing().when(riskValidationService).validateTrade(any(), any(), anyDouble());
            doThrow(new RuntimeException("Kafka unavailable"))
                    .when(tradeEventProducer).publish(any());

            // Act — should NOT throw
            Order result = orderService.buyAsset(bitcoin, 0.5, customer);

            // Assert — order is still FILLED despite Kafka failure
            assertThat(result.getStatus()).isEqualTo(OrderStatus.FILLED);
        }
    }

    // =========================================================================
    //  SELL ASSET
    // =========================================================================
    @Nested
    @DisplayName("sellAsset")
    class SellAsset {

        private Asset ownedAsset;

        @BeforeEach
        void setUpSellScenario() {
            // User owns 2.0 BTC purchased at 40,000
            ownedAsset = new Asset();
            ownedAsset.setId(88L);
            ownedAsset.setQuantity(2.0);
            ownedAsset.setBuyPrice(40_000.0);
        }

        @Test
        @DisplayName("Happy path: SELL fills order, credits wallet, updates asset, publishes event")
        void shouldFillSellOrder_creditWallet_updateAsset() {
            // Arrange — after selling 1.0 BTC, still holds 1.0 BTC > dust threshold
            Asset updatedAsset = new Asset();
            updatedAsset.setId(88L);
            updatedAsset.setQuantity(1.0);
            updatedAsset.setBuyPrice(40_000.0);
            when(walletService.getUserWallet(customer)).thenReturn(wallet);
            when(assetService.findAssetByUserIdAndCoinId(customer.getId(), bitcoin.getId()))
                    .thenReturn(ownedAsset);
            when(walletService.credit(any(), any(), any(), any())).thenReturn(wallet);
            doNothing().when(riskValidationService).validateTrade(any(), any(), anyDouble());
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(assetService.updateAsset(eq(88L), eq(-1.0))).thenReturn(updatedAsset);

            // Act
            Order result = orderService.sellAsset(bitcoin, 1.0, customer);

            // Assert
            assertThat(result.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(result.getOrderType()).isEqualTo(OrderType.SELL);

            verify(walletService).credit(eq(customer), any(), any(), any());
            verify(feeService).deductFee(eq(customer), any(), any(), any());
            verify(assetService).updateAsset(eq(88L), eq(-1.0));
            verify(tradeEventProducer).publish(any());
        }

        @Test
        @DisplayName("Deletes dust asset when remaining value falls below $1")
        void shouldDeleteAsset_whenRemainingValueIsBelowDustThreshold() {
            // Arrange — sell almost all BTC, leaving dust
            // Current price = 50,000. Remaining = 0.00001 BTC = $0.50 < $1 threshold
            Asset dustAsset = new Asset();
            dustAsset.setId(88L);
            dustAsset.setQuantity(0.00001);
            dustAsset.setBuyPrice(40_000.0);

            when(walletService.getUserWallet(customer)).thenReturn(wallet);
            when(assetService.findAssetByUserIdAndCoinId(customer.getId(), bitcoin.getId()))
                    .thenReturn(ownedAsset);
            when(walletService.credit(any(), any(), any(), any())).thenReturn(wallet);
            doNothing().when(riskValidationService).validateTrade(any(), any(), anyDouble());
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(walletService.credit(any(), any(), any(), any())).thenReturn(wallet);
            when(assetService.updateAsset(eq(88L), anyDouble())).thenReturn(dustAsset);

            // Act
            orderService.sellAsset(bitcoin, 1.9999, customer);

            // Assert — dust asset deleted
            verify(assetService).deleteAsset(88L);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when user does not own the coin")
        void shouldThrow_whenUserDoesNotOwnCoin() {
            when(assetService.findAssetByUserIdAndCoinId(customer.getId(), bitcoin.getId()))
                    .thenReturn(null);

            assertThatThrownBy(() -> orderService.sellAsset(bitcoin, 0.5, customer))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Throws OrderValidationException when selling more than owned quantity")
        void shouldThrow_whenSellingMoreThanOwned() {
            // User owns 2.0 BTC, tries to sell 3.0
            when(assetService.findAssetByUserIdAndCoinId(customer.getId(), bitcoin.getId()))
                    .thenReturn(ownedAsset);
            assertThatThrownBy(() -> orderService.sellAsset(bitcoin, 3.0, customer))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("Insufficient asset quantity");
        }

        @Test
        @DisplayName("Throws OrderValidationException when quantity is zero or negative")
        void shouldThrow_whenQuantityIsNotPositive() {
            assertThatThrownBy(() -> orderService.sellAsset(bitcoin, 0.0, customer))
                    .isInstanceOf(OrderValidationException.class);
        }
    }

    // =========================================================================
    //  processOrder (routing)
    // =========================================================================
    @Nested
    @DisplayName("processOrder")
    class ProcessOrder {

        @Test
        @DisplayName("Routes BUY type to buyAsset")
        void shouldRouteToBuyAsset_forBuyOrderType() {
            // Arrange — spy to verify routing without re-testing buy logic
            OrderServiceImpl spy = spy(orderService);
            doReturn(new Order()).when(spy).buyAsset(any(), anyDouble(), any());

            spy.processOrder(bitcoin, 0.5, OrderType.BUY, customer);

            verify(spy).buyAsset(bitcoin, 0.5, customer);
            verify(spy, never()).sellAsset(any(), anyDouble(), any());
        }

        @Test
        @DisplayName("Routes SELL type to sellAsset")
        void shouldRouteToSellAsset_forSellOrderType() {
            OrderServiceImpl spy = spy(orderService);
            doReturn(new Order()).when(spy).sellAsset(any(), anyDouble(), any());

            spy.processOrder(bitcoin, 1.0, OrderType.SELL, customer);

            verify(spy).sellAsset(bitcoin, 1.0, customer);
            verify(spy, never()).buyAsset(any(), anyDouble(), any());
        }
    }

    // =========================================================================
    //  cancelOrder
    // =========================================================================
    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("Happy path: cancels OPEN BUY order and releases locked funds")
        void shouldCancelOrder_andReleaseLocks_forOpenBuyOrder() {
            // Arrange
            OrderItem item = new OrderItem();
            item.setQuantity(0.5);

            Order order = new Order();
            order.setId(100L);
            order.setUser(customer);
            order.setOrderType(OrderType.BUY);
            order.setStatus(OrderStatus.OPEN);
            order.setPrice(BigDecimal.valueOf(25_000));
            order.setRemainingQuantity(0.5);
            order.setOrderItem(item);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Order cancelled = orderService.cancelOrder(100L, customer);

            // Assert — status is CANCELLED and lock released
            assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(walletService).releaseLock(eq(customer), any(), any(), any());
            verify(orderRepository).save(any());
        }

        @Test
        @DisplayName("Throws UnauthorizedAccessException when a different user tries to cancel")
        void shouldThrow_whenCancellerIsNotOrderOwner() {
            User anotherUser = TestDataBuilder.buildCustomer(99L, "other@trading.com");

            Order order = new Order();
            order.setId(100L);
            order.setUser(customer);     // belongs to customer, not anotherUser
            order.setStatus(OrderStatus.OPEN);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, anotherUser))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("Throws OrderValidationException when order is already FILLED")
        void shouldThrow_whenOrderAlreadyFilled() {
            Order order = new Order();
            order.setId(100L);
            order.setUser(customer);
            order.setStatus(OrderStatus.FILLED);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, customer))
                    .isInstanceOf(OrderValidationException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        @DisplayName("Throws OrderValidationException when order is already CANCELLED")
        void shouldThrow_whenOrderAlreadyCancelled() {
            Order order = new Order();
            order.setId(100L);
            order.setUser(customer);
            order.setStatus(OrderStatus.CANCELLED);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, customer))
                    .isInstanceOf(OrderValidationException.class);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when orderId does not exist")
        void shouldThrow_whenOrderIdNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(999L, customer))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Does NOT release lock for SELL order cancellation")
        void shouldNotReleaseLock_whenCancellingSellOrder() {
            Order order = new Order();
            order.setId(100L);
            order.setUser(customer);
            order.setOrderType(OrderType.SELL);
            order.setStatus(OrderStatus.OPEN);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            orderService.cancelOrder(100L, customer);

            // No wallet lock released for SELL orders
            verify(walletService, never()).releaseLock(any(), any(), any(), any());
            verify(orderRepository).save(any());
        }
        }
    }
