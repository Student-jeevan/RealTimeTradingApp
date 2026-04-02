package com.jeevan.TradingApp.integration;

import com.jeevan.TradingApp.domain.USER_ROLE;
import com.jeevan.TradingApp.domain.WithdrawalStatus;
import com.jeevan.TradingApp.modal.*;
import com.jeevan.TradingApp.repository.*;
import com.jeevan.TradingApp.service.WalletService;
import com.jeevan.TradingApp.service.WithdrawalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the complete Withdrawal lifecycle:
 *
 *  1. User requests withdrawal → PENDING, funds locked from wallet
 *  2. Admin approves → SUCCESS, Kafka notification queued
 *  3. Admin rejects → DECLINE, funds refunded to user's wallet
 *
 * Uses real MySQL + Kafka containers. JavaMailSender is mocked via
 * @MockBean to prevent real SMTP calls during integration tests.
 */
@DisplayName("Withdrawal Integration Tests")
class WithdrawalIntegrationTest extends BaseIntegrationTest {

    @Autowired private WithdrawalService withdrawalService;
    @Autowired private WalletService walletService;

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WithdrawalRepository withdrawalRepository;
    @Autowired private PaymentDetailsRepository paymentDetailsRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @Autowired private WalletLedgerRepository walletLedgerRepository;

    // Beans replaced with no-op mocks so emails don't actually send
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        customer = createAndPersistUser("withdraw_customer_" + System.nanoTime() + "@test.com",
                USER_ROLE.ROLE_CUSTOMER);
        admin = createAndPersistUser("withdraw_admin_" + System.nanoTime() + "@test.com",
                USER_ROLE.ROLE_ADMIN);

        // Fund the customer wallet
        walletService.addBalance(customer, BigDecimal.valueOf(10_000));

        // Customer must have payment details to request withdrawal
        PaymentDetails pd = new PaymentDetails();
        pd.setUser(customer);
        pd.setAccountNumber("1234567890");
        pd.setIfsc("HDFC0001234");
        pd.setBankName("HDFC Bank");
        pd.setAccountHolderName("Integration Customer");
        paymentDetailsRepository.save(pd);
    }

    @AfterEach
    void tearDown() {
        // Delete in FK-safe order: children before parents
        walletLedgerRepository.deleteAll();
        withdrawalRepository.deleteAll();
        paymentDetailsRepository.deleteAll();
        walletRepository.deleteAll();
        processedEventRepository.deleteAll();
        userRepository.delete(customer);
        userRepository.delete(admin);
    }

    // =========================================================================
    //  Request Withdrawal
    // =========================================================================
    @Nested
    @DisplayName("Request Withdrawal")
    class RequestWithdrawal {

        @Test
        @DisplayName("Creates PENDING withdrawal and locks funds in wallet immediately")
        void request_shouldCreatePendingWithdrawal_andLockFunds() {
            // Arrange
            Wallet walletBefore = walletService.getUserWallet(customer);
            BigDecimal balanceBefore = walletBefore.getBalance(); // 10,000

            // Act
            Withdrawal result = withdrawalService.requestWithdrawal(2_000L, customer);

            // Assert — withdrawal status
            assertThat(result.getId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
            assertThat(result.getAmount()).isEqualTo(2_000L);

            // Assert — persisted in DB
            Withdrawal fromDb = withdrawalRepository.findById(result.getId()).orElseThrow();
            assertThat(fromDb.getStatus()).isEqualTo(WithdrawalStatus.PENDING);

            // Assert — funds deducted from wallet (locked)
            Wallet walletAfter = walletService.getUserWallet(customer);
            assertThat(walletAfter.getBalance())
                    .isEqualByComparingTo(balanceBefore.subtract(BigDecimal.valueOf(2_000)));
        }

        @Test
        @DisplayName("Throws when amount exceeds wallet balance")
        void request_shouldThrow_whenBalanceInsufficient() {
            // Wallet has 10,000 — try to withdraw 50,000
            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(50_000L, customer))
                    .isInstanceOf(RuntimeException.class);

            // Wallet balance unchanged
            Wallet wallet = walletService.getUserWallet(customer);
            assertThat(wallet.getBalance()).isEqualByComparingTo("10000");

            // No withdrawal record created
            assertThat(withdrawalRepository.findByUserId(customer.getId())).isEmpty();
        }
    }

    // =========================================================================
    //  Approve Withdrawal
    // =========================================================================
    @Nested
    @DisplayName("Approve Withdrawal")
    class ApproveWithdrawal {

        private Withdrawal pending;

        @BeforeEach
        void createPendingWithdrawal() {
            pending = withdrawalService.requestWithdrawal(1_000L, customer);
        }

        @Test
        @DisplayName("Approval sets status SUCCESS, sets audit fields, publishes Kafka notification")
        void approve_shouldSetSuccess_andPublishNotification() throws InterruptedException {
            // Act
            Withdrawal approved = withdrawalService.approveWithdrawal(pending.getId(), admin);

            // Assert — withdrawal fields
            assertThat(approved.getStatus()).isEqualTo(WithdrawalStatus.SUCCESS);
            assertThat(approved.getApprovedAt()).isNotNull();
            assertThat(approved.getApprovedBy()).isEqualTo(admin.getEmail());

            // Assert — DB persisted
            Withdrawal fromDb = withdrawalRepository.findById(pending.getId()).orElseThrow();
            assertThat(fromDb.getStatus()).isEqualTo(WithdrawalStatus.SUCCESS);
            assertThat(fromDb.getApprovedBy()).isEqualTo(admin.getEmail());

            // Assert — Kafka notification consumed (wait up to 5s for async consumer)
            waitFor(() -> processedEventRepository.count() >= 1, 5_000);
            assertThat(processedEventRepository.count()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Double approval throws WITHDRAWAL_ALREADY_PROCESSED")
        void doubleApprove_shouldThrowWithoutModifyingState() {
            withdrawalService.approveWithdrawal(pending.getId(), admin);

            // Second attempt must fail
            assertThatThrownBy(() -> withdrawalService.approveWithdrawal(pending.getId(), admin))
                    .isInstanceOf(com.jeevan.TradingApp.exception.CustomException.class)
                    .hasMessageContaining("already");
        }

        @Test
        @DisplayName("Non-admin cannot approve withdrawal")
        void approvalByCustomer_shouldThrowUnauthorized() {
            assertThatThrownBy(() -> withdrawalService.approveWithdrawal(pending.getId(), customer))
                    .isInstanceOf(com.jeevan.TradingApp.exception.UnauthorizedAccessException.class);

            // Withdrawal still PENDING in DB
            Withdrawal fromDb = withdrawalRepository.findById(pending.getId()).orElseThrow();
            assertThat(fromDb.getStatus()).isEqualTo(WithdrawalStatus.PENDING);
        }
    }

    // =========================================================================
    //  Reject Withdrawal
    // =========================================================================
    @Nested
    @DisplayName("Reject Withdrawal")
    class RejectWithdrawal {

        private Withdrawal pending;
        private BigDecimal walletBalanceBeforeRequest;

        @BeforeEach
        void createPendingWithdrawal() {
            walletBalanceBeforeRequest = walletService.getUserWallet(customer).getBalance();
            pending = withdrawalService.requestWithdrawal(3_000L, customer);
        }

        @Test
        @DisplayName("Rejection sets DECLINE, refunds full amount to owner wallet")
        void reject_shouldSetDecline_andRefundOwnerWallet() {
            // Wallet currently has 10,000 - 3,000 = 7,000
            BigDecimal balanceAfterRequest = walletService.getUserWallet(customer).getBalance();
            assertThat(balanceAfterRequest).isEqualByComparingTo("7000");

            // Act — admin rejects
            Withdrawal rejected = withdrawalService.rejectWithdrawal(pending.getId(), admin);

            // Assert — withdrawal declined
            assertThat(rejected.getStatus()).isEqualTo(WithdrawalStatus.DECLINE);

            // Assert — funds refunded
            Wallet walletAfterRefund = walletService.getUserWallet(customer);
            assertThat(walletAfterRefund.getBalance())
                    .isEqualByComparingTo(walletBalanceBeforeRequest); // back to 10,000
        }

        @Test
        @DisplayName("Funds refunded to OWNER's wallet, not admin's wallet")
        void reject_shouldRefundToOwner_notAdmin() {
            Wallet adminWalletBefore = walletService.getUserWallet(admin);
            BigDecimal adminBalanceBefore = adminWalletBefore.getBalance();

            withdrawalService.rejectWithdrawal(pending.getId(), admin);

            // Admin wallet balance unchanged
            Wallet adminWalletAfter = walletService.getUserWallet(admin);
            assertThat(adminWalletAfter.getBalance())
                    .isEqualByComparingTo(adminBalanceBefore);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User createAndPersistUser(String email, USER_ROLE role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName("Test " + role.name());
        u.setPassword("hashed_pass");
        u.setRole(role);
        u.setVerified(true);
        return userRepository.save(u);
    }

    private void waitFor(java.util.function.BooleanSupplier condition, long maxWaitMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(200);
        }
    }
}
