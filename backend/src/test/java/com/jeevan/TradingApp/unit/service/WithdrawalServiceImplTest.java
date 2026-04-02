package com.jeevan.TradingApp.unit.service;

import com.jeevan.TradingApp.domain.WithdrawalStatus;
import com.jeevan.TradingApp.exception.CustomException;
import com.jeevan.TradingApp.exception.InsufficientBalanceException;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import com.jeevan.TradingApp.kafka.producer.NotificationEventProducer;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.Withdrawal;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.repository.WithdrawalRepository;
import com.jeevan.TradingApp.service.PaymentDetailsService;
import com.jeevan.TradingApp.service.WalletService;
import com.jeevan.TradingApp.service.WithdrawalServiceImpl;
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
 * Unit tests for WithdrawalServiceImpl.
 *
 * Critical assertions:
 *  - requestWithdrawal: validates payment details, balance, locks funds
 *  - approveWithdrawal: admin-only, PENDING-only guard, Kafka notification
 *  - rejectWithdrawal: admin-only, PENDING-only, refunds owner's wallet
 *  - Notification failure must NEVER roll back the DB transaction
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawalServiceImpl Unit Tests")
class WithdrawalServiceImplTest {

    @Mock private WalletService walletService;
    @Mock private WalletRepository walletRepository;
    @Mock private WithdrawalRepository withdrawalRepository;
    @Mock private PaymentDetailsService paymentDetailsService;
    @Mock private NotificationEventProducer notificationProducer;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    private User customer;
    private User admin;
    private Wallet customerWallet;

    @BeforeEach
    void setUp() {
        customer       = TestDataBuilder.buildCustomer();
        admin          = TestDataBuilder.buildAdmin();
        customerWallet = TestDataBuilder.buildWallet(customer, BigDecimal.valueOf(5_000));
    }

    // =========================================================================
    //  requestWithdrawal
    // =========================================================================
    @Nested
    @DisplayName("requestWithdrawal")
    class RequestWithdrawal {

        @BeforeEach
        void setupPaymentDetails() {
            // Payment details exist by default
            when(paymentDetailsService.getUsersPaymentDetails(customer))
                    .thenReturn(new com.jeevan.TradingApp.modal.PaymentDetails());
        }

        @Test
        @DisplayName("Happy path: creates PENDING withdrawal, deducts wallet balance")
        void shouldCreatePendingWithdrawal_andDeductWalletBalance() {
            // Arrange
            Long amount = 1_000L;
            BigDecimal before = customerWallet.getBalance(); // 5,000

            when(walletService.getUserWallet(customer)).thenReturn(customerWallet);
            when(withdrawalRepository.save(any(Withdrawal.class)))
                    .thenAnswer(inv -> {
                        Withdrawal w = inv.getArgument(0);
                        if (w.getId() == null) w.setId(20L);
                        return w;
                    });

            // Act
            Withdrawal result = withdrawalService.requestWithdrawal(amount, customer);

            // Assert — withdrawal is PENDING
            assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
            assertThat(result.getAmount()).isEqualTo(amount);

            // Wallet balance reduced (funds locked)
            assertThat(customerWallet.getBalance())
                    .isEqualByComparingTo(before.subtract(BigDecimal.valueOf(amount)));

            verify(walletRepository).save(customerWallet);
            verify(withdrawalRepository).save(any(Withdrawal.class));
        }

        @Test
        @DisplayName("Throws PAYMENT_DETAILS_MISSING when user has no payment details")
        void shouldThrow_whenPaymentDetailsMissing() {
            when(paymentDetailsService.getUsersPaymentDetails(customer)).thenReturn(null);

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(1_000L, customer))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("PAYMENT_DETAILS_MISSING");

            verifyNoInteractions(withdrawalRepository);
        }

        @Test
        @DisplayName("Throws InsufficientBalanceException when wallet balance < requested amount")
        void shouldThrow_whenInsufficientBalance() {
            Long hugeAmount = 999_999L;  // more than wallet's 5,000
            when(walletService.getUserWallet(customer)).thenReturn(customerWallet);

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(hugeAmount, customer))
                    .isInstanceOf(InsufficientBalanceException.class);

            verifyNoInteractions(withdrawalRepository);
        }

        @Test
        @DisplayName("Withdrawal date is always set to current time")
        void shouldSetCurrentDateOnWithdrawal() {
            when(walletService.getUserWallet(customer)).thenReturn(customerWallet);
            when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> inv.getArgument(0));

            Withdrawal result = withdrawalService.requestWithdrawal(500L, customer);
            assertThat(result.getDate()).isNotNull();
        }
    }

    // =========================================================================
    //  approveWithdrawal
    // =========================================================================
    @Nested
    @DisplayName("approveWithdrawal")
    class ApproveWithdrawal {

        private Withdrawal pendingWithdrawal;

        @BeforeEach
        void setUpPendingWithdrawal() {
            pendingWithdrawal = TestDataBuilder.buildPendingWithdrawal(customer, 1_000L);
        }

        @Test
        @DisplayName("Happy path: status → SUCCESS, audit fields set, Kafka notification published")
        void shouldApproveWithdrawal_andPublishNotification() {
            // Arrange
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));
            when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Withdrawal result = withdrawalService.approveWithdrawal(20L, admin);

            // Assert — status changed
            assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.SUCCESS);
            assertThat(result.getApprovedAt()).isNotNull();
            assertThat(result.getApprovedBy()).isEqualTo(admin.getEmail());

            // Kafka notification must be published
            verify(notificationProducer).publish(any());
            verify(withdrawalRepository).save(any());
        }

        @Test
        @DisplayName("Throws UnauthorizedAccessException when approver is not ADMIN")
        void shouldThrow_whenApproverIsCustomer() {
            assertThatThrownBy(() -> withdrawalService.approveWithdrawal(20L, customer))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("Only admins");

            // Withdrawal must NOT be updated
            verify(withdrawalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws WITHDRAWAL_ALREADY_PROCESSED when withdrawal is already SUCCESS")
        void shouldThrow_whenWithdrawalAlreadyApproved() {
            pendingWithdrawal.setStatus(WithdrawalStatus.SUCCESS);
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> withdrawalService.approveWithdrawal(20L, admin))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("WITHDRAWAL_ALREADY_PROCESSED");
        }

        @Test
        @DisplayName("Throws WITHDRAWAL_ALREADY_PROCESSED when withdrawal is DECLINED")
        void shouldThrow_whenWithdrawalAlreadyDeclined() {
            pendingWithdrawal.setStatus(WithdrawalStatus.DECLINE);
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> withdrawalService.approveWithdrawal(20L, admin))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("Notification publish failure does NOT fail the approval transaction")
        void notificationFailure_shouldNotRollbackApproval() {
            // Arrange — Kafka producer throws
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));
            when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Kafka down"))
                    .when(notificationProducer).publish(any());

            // Act — should NOT throw despite notification failure
            Withdrawal result = withdrawalService.approveWithdrawal(20L, admin);

            // Assert — withdrawal still approved
            assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.SUCCESS);
            verify(withdrawalRepository).save(any());
        }
    }

    // =========================================================================
    //  rejectWithdrawal
    // =========================================================================
    @Nested
    @DisplayName("rejectWithdrawal")
    class RejectWithdrawal {

        private Withdrawal pendingWithdrawal;

        @BeforeEach
        void setUpPendingWithdrawal() {
            pendingWithdrawal = TestDataBuilder.buildPendingWithdrawal(customer, 1_000L);
        }

        @Test
        @DisplayName("Happy path: refunds owner wallet, status → DECLINE, notification published")
        void shouldDeclineWithdrawal_refundWallet_andNotify() {
            // Arrange
            Wallet ownerWallet = TestDataBuilder.buildWallet(customer, BigDecimal.valueOf(4_000));
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));
            when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletService.getUserWallet(customer)).thenReturn(ownerWallet);

            // Act
            Withdrawal result = withdrawalService.rejectWithdrawal(20L, admin);

            // Assert — status is DECLINE
            assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.DECLINE);
            assertThat(result.getApprovedBy()).isEqualTo(admin.getEmail());

            // Wallet refunded (4,000 + 1,000 = 5,000)
            assertThat(ownerWallet.getBalance())
                    .isEqualByComparingTo(BigDecimal.valueOf(5_000));
            verify(walletRepository).save(ownerWallet);

            // Kafka notification
            verify(notificationProducer).publish(any());
        }

        @Test
        @DisplayName("Throws UnauthorizedAccessException when rejector is not ADMIN")
        void shouldThrow_whenRejectorIsCustomer() {
            assertThatThrownBy(() -> withdrawalService.rejectWithdrawal(20L, customer))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("Reverts funds to OWNER's wallet, not admin's wallet")
        void shouldRefundToOwner_notAdmin() {
            Wallet adminWallet  = TestDataBuilder.buildWallet(admin, BigDecimal.valueOf(0));
            Wallet ownerWallet  = TestDataBuilder.buildWallet(customer, BigDecimal.valueOf(4_000));

            // Only customer's wallet is returned
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));
            when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletService.getUserWallet(customer)).thenReturn(ownerWallet);

            withdrawalService.rejectWithdrawal(20L, admin);

            // Owner wallet saved (refunded)
            verify(walletRepository).save(ownerWallet);
            // Admin wallet NOT touched
            verify(walletRepository, never()).save(adminWallet);
        }

        @Test
        @DisplayName("Throws when rejection attempted on already-approved withdrawal")
        void shouldThrow_whenAlreadyApproved() {
            pendingWithdrawal.setStatus(WithdrawalStatus.SUCCESS);
            when(withdrawalRepository.findById(20L)).thenReturn(Optional.of(pendingWithdrawal));

            assertThatThrownBy(() -> withdrawalService.rejectWithdrawal(20L, admin))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("WITHDRAWAL_ALREADY_PROCESSED");
        }
    }
}
