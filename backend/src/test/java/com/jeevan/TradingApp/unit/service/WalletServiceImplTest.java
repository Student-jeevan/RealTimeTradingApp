package com.jeevan.TradingApp.unit.service;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.exception.InsufficientBalanceException;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.repository.UserRepository;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.service.FeeService;
import com.jeevan.TradingApp.service.LedgerService;
import com.jeevan.TradingApp.service.WalletServiceImpl;
import com.jeevan.TradingApp.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WalletServiceImpl.
 *
 * Every wallet operation must:
 *  1. Correctly calculate balance changes
 *  2. Create the matching WalletLedger entry
 *  3. Persist the updated Wallet
 *  4. Guard against over-spending (InsufficientBalanceException)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletServiceImpl Unit Tests")
@SuppressWarnings("null") // Mockito stubs return unannotated types that Eclipse flags as @NonNull violations
class WalletServiceImplTest {

    @Mock private WalletRepository walletRepository;
    @Mock private LedgerService ledgerService;
    @Mock private FeeService feeService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User customer;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        customer = TestDataBuilder.buildCustomer();
        wallet   = TestDataBuilder.buildWallet(customer, BigDecimal.valueOf(10_000));
    }

    // =========================================================================
    //  getUserWallet
    // =========================================================================
    @Nested
    @DisplayName("getUserWallet")
    class GetUserWallet {

        @Test
        @DisplayName("Returns existing wallet when found")
        void shouldReturnExistingWallet() {
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            Wallet result = walletService.getUserWallet(customer);

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo("10000");
        }

        @Test
        @DisplayName("Creates and saves new wallet when user has none")
        void shouldCreateNewWallet_whenNoneExists() {
            when(walletRepository.findByUserId(customer.getId())).thenReturn(null);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
                Wallet w = inv.getArgument(0);
                w.setId(1L);
                return w;
            });

            Wallet result = walletService.getUserWallet(customer);

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Computes availableBalance = balance - lockedBalance on every fetch")
        void shouldPopulateAvailableBalance() {
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            wallet.setBalance(BigDecimal.valueOf(10_000));
            wallet.setLockedBalance(BigDecimal.valueOf(3_000));

            Wallet result = walletService.getUserWallet(customer);

            assertThat(result.getAvailableBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(7_000));
        }
    }

    // =========================================================================
    //  addBalance
    // =========================================================================
    @Nested
    @DisplayName("addBalance")
    class AddBalance {

        @Test
        @DisplayName("Increases balance and creates CREDIT ledger entry")
        void shouldIncreaseBalance_andCreateCreditLedgerEntry() {
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
            // Act
            Wallet result = walletService.addBalance(customer, BigDecimal.valueOf(5_000));

            // Assert — balance increased from 10,000 → 15,000
            assertThat(result.getBalance()).isEqualByComparingTo("15000");

            // Ledger entry must be CREDIT type
            verify(ledgerService).createLedgerEntry(
                    eq(customer),
                    eq(LedgerTransactionType.CREDIT),
                    eq(BigDecimal.valueOf(5_000)),
                    isNull(),
                    anyString()
            );
        }

        @Test
        @DisplayName("Adds to existing non-zero balance (cumulative)")
        void shouldCumulateBalance_onMultipleDeposits() {
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
            walletService.addBalance(customer, BigDecimal.valueOf(1_000));
            walletService.addBalance(customer, BigDecimal.valueOf(2_000));

            assertThat(wallet.getBalance()).isEqualByComparingTo("13000");
        }
    }

    // =========================================================================
    //  debit
    // =========================================================================
    @Nested
    @DisplayName("debit")
    class Debit {

        @Test
        @DisplayName("Deducts amount from balance and creates DEBIT ledger entry")
        void shouldDeductBalance_andCreateDebitEntry() {
            // Arrange
            BigDecimal debitAmount = BigDecimal.valueOf(2_000);
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Wallet result = walletService.debit(customer, debitAmount, "order-1", "Execute BUY");

            // Assert
            assertThat(result.getBalance()).isEqualByComparingTo("8000");
            verify(ledgerService).createLedgerEntry(
                    eq(customer),
                    eq(LedgerTransactionType.DEBIT),
                    eq(debitAmount),
                    eq("order-1"),
                    eq("Execute BUY")
            );
        }
    }

    // =========================================================================
    //  credit
    // =========================================================================
    @Nested
    @DisplayName("credit")
    class Credit {

        @Test
        @DisplayName("Adds amount to balance and creates CREDIT ledger entry")
        void shouldAddBalance_andCreateCreditEntry() {
            BigDecimal creditAmount = BigDecimal.valueOf(3_000);
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            Wallet result = walletService.credit(customer, creditAmount, "order-2", "Execute SELL");

            assertThat(result.getBalance()).isEqualByComparingTo("13000");
            verify(ledgerService).createLedgerEntry(
                    eq(customer),
                    eq(LedgerTransactionType.CREDIT),
                    eq(creditAmount),
                    eq("order-2"),
                    eq("Execute SELL")
            );
        }
    }

    // =========================================================================
    //  releaseLock
    // =========================================================================
    @Nested
    @DisplayName("releaseLock")
    class ReleaseLock {

        @Test
        @DisplayName("Decrements lockedBalance and creates TRADE_RELEASE entry")
        void shouldDecrementLockedBalance_andCreateReleaseEntry() {
            wallet.setLockedBalance(BigDecimal.valueOf(5_000));
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            walletService.releaseLock(customer, BigDecimal.valueOf(5_000), "order-1", "Release lock");

            assertThat(wallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(ledgerService).createLedgerEntry(
                    eq(customer),
                    eq(LedgerTransactionType.TRADE_RELEASE),
                    any(),
                    eq("order-1"),
                    anyString()
            );
        }
    }

    // =========================================================================
    //  payOrderPayment (BUY order)
    // =========================================================================
    @Nested
    @DisplayName("payOrderPayment")
    class PayOrderPayment {

        @Test
        @DisplayName("Locks funds for BUY order when available balance is sufficient")
        void shouldLockFunds_whenAvailableBalanceSufficient() {
            // Arrange
            Order buyOrder = buildBuyOrder(BigDecimal.valueOf(5_000));
            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerService.calculateAvailableBalance(customer.getId()))
                    .thenReturn(BigDecimal.valueOf(10_000));

            // Act
            walletService.payOrderPayment(buyOrder, customer);

            // Assert — TRADE_LOCK ledger entry created
            verify(ledgerService).createLedgerEntry(
                    eq(customer),
                    eq(LedgerTransactionType.TRADE_LOCK),
                    eq(BigDecimal.valueOf(5_000)),
                    anyString(),
                    anyString()
            );
        }

        @Test
        @DisplayName("Throws InsufficientBalanceException for BUY when available balance is too low")
        void shouldThrow_whenAvailableBalanceTooLow() {
            Order buyOrder = buildBuyOrder(BigDecimal.valueOf(20_000));
            when(ledgerService.calculateAvailableBalance(customer.getId()))
                    .thenReturn(BigDecimal.valueOf(5_000));  // only $5,000 available

            assertThatThrownBy(() -> walletService.payOrderPayment(buyOrder, customer))
                    .isInstanceOf(InsufficientBalanceException.class);

            // No lock must be created
            verify(ledgerService, never()).createLedgerEntry(any(), any(), any(), any(), any());
        }
    }

    // =========================================================================
    //  walletToWalletTransfer
    // =========================================================================
    @Nested
    @DisplayName("walletToWalletTransfer")
    class WalletToWalletTransfer {

        @Test
        @DisplayName("Transfers amount from sender to receiver wallets")
        void shouldDebitSenderAndCreditReceiver() {
            // Arrange
            User receiver = TestDataBuilder.buildCustomer(2L, "receiver@trading.com");
            Wallet receiverWallet = TestDataBuilder.buildWallet(receiver, BigDecimal.valueOf(1_000));
            receiverWallet.setId(20L);

            when(walletRepository.findByUserId(customer.getId())).thenReturn(wallet);
            when(ledgerService.calculateAvailableBalance(customer.getId()))
                    .thenReturn(BigDecimal.valueOf(10_000));

            // Act
            walletService.walletToWalletTransfer(customer, receiverWallet, 3_000L);

            // Assert DEBIT for sender
            verify(ledgerService).createLedgerEntry(
                    eq(customer), eq(LedgerTransactionType.DEBIT), any(), isNull(), anyString());
            // Assert CREDIT for receiver
            verify(ledgerService).createLedgerEntry(
                    eq(receiver), eq(LedgerTransactionType.CREDIT), any(), isNull(), anyString());
        }

        @Test
        @DisplayName("Throws InsufficientBalanceException when sender cannot cover the transfer")
        void shouldThrow_whenSenderHasInsufficientFunds() {
            when(ledgerService.calculateAvailableBalance(customer.getId()))
                    .thenReturn(BigDecimal.valueOf(500));  // can't cover 3000

            User receiver = TestDataBuilder.buildCustomer(2L, "receiver@trading.com");
            Wallet receiverWallet = TestDataBuilder.buildWallet(receiver, BigDecimal.ZERO);

            assertThatThrownBy(() -> walletService.walletToWalletTransfer(customer, receiverWallet, 3_000L))
                    .isInstanceOf(InsufficientBalanceException.class);
        }
    }

    // =========================================================================
    //  findWalletById
    // =========================================================================
    @Nested
    @DisplayName("findWalletById")
    class FindWalletById {

        @Test
        @DisplayName("Returns wallet when ID exists")
        void shouldReturnWallet_whenFound() {
            when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
            Wallet found = walletService.findWalletById(10L);
            assertThat(found).isEqualTo(wallet);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when wallet ID not found")
        void shouldThrow_whenNotFound() {
            when(walletRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> walletService.findWalletById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Order buildBuyOrder(BigDecimal price) {
        Order order = new Order();
        order.setId(100L);
        order.setUser(customer);
        order.setOrderType(OrderType.BUY);
        order.setPrice(price);
        return order;
    }
}
