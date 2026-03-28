package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.USER_ROLE;
import com.jeevan.TradingApp.domain.WithdrawalStatus;
import com.jeevan.TradingApp.exception.CustomException;
import com.jeevan.TradingApp.exception.InsufficientBalanceException;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import com.jeevan.TradingApp.kafka.events.NotificationEvent;
import com.jeevan.TradingApp.kafka.producer.NotificationEventProducer;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.Withdrawal;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.repository.WithdrawalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WithdrawalServiceImpl implements WithdrawalService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawalServiceImpl.class);

    @Autowired private WalletService walletService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WithdrawalRepository withdrawalRepository;
    @Autowired private PaymentDetailsService paymentDetailsService;
    @Autowired private NotificationEventProducer notificationProducer;

    // ──────────────────────────────────────────────
    // USER: request withdrawal
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public Withdrawal requestWithdrawal(Long amount, User user) {
        if (paymentDetailsService.getUsersPaymentDetails(user) == null) {
            throw new CustomException(
                "Please add payment details first before requesting withdrawal.",
                "PAYMENT_DETAILS_MISSING");
        }

        Wallet userWallet = walletService.getUserWallet(user);

        if (userWallet.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new InsufficientBalanceException(
                "Wallet balance is insufficient to request this withdrawal");
        }

        // Lock funds immediately so they can't be spent while awaiting approval
        userWallet.setBalance(userWallet.getBalance().subtract(BigDecimal.valueOf(amount)));
        walletRepository.save(userWallet);

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setAmount(amount);
        withdrawal.setUser(user);
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setDate(LocalDateTime.now());

        Withdrawal saved = withdrawalRepository.save(withdrawal);
        log.info("[Withdrawal] User {} requested withdrawal of {} — id={}", user.getId(), amount, saved.getId());
        return saved;
    }

    // ──────────────────────────────────────────────
    // ADMIN: approve withdrawal
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public Withdrawal approveWithdrawal(Long withdrawalId, User admin) {
        assertAdmin(admin);

        Withdrawal withdrawal = getWithdrawal(withdrawalId);
        assertPending(withdrawal);

        // Mark as approved
        withdrawal.setStatus(WithdrawalStatus.SUCCESS);
        withdrawal.setApprovedAt(LocalDateTime.now());
        withdrawal.setApprovedBy(admin.getEmail());

        Withdrawal saved = withdrawalRepository.save(withdrawal);

        log.info("[Withdrawal] Admin {} approved withdrawal id={} for user={}, amount={}",
                admin.getEmail(), withdrawalId, withdrawal.getUser().getId(), withdrawal.getAmount());

        // Notify user via Kafka
        publishWithdrawalNotification(withdrawal.getUser(),
                "Withdrawal Approved ✅",
                "Your withdrawal of $" + withdrawal.getAmount() + " has been approved and is being processed.",
                "WITHDRAWAL_APPROVED");

        return saved;
    }

    // ──────────────────────────────────────────────
    // ADMIN: reject withdrawal
    // ──────────────────────────────────────────────

    @Override
    @Transactional
    public Withdrawal rejectWithdrawal(Long withdrawalId, User admin) {
        assertAdmin(admin);

        Withdrawal withdrawal = getWithdrawal(withdrawalId);
        assertPending(withdrawal);

        // Refund locked funds back to the WITHDRAWAL OWNER's wallet (not the admin's)
        User owner = withdrawal.getUser();
        Wallet ownerWallet = walletService.getUserWallet(owner);
        ownerWallet.setBalance(ownerWallet.getBalance().add(BigDecimal.valueOf(withdrawal.getAmount())));
        walletRepository.save(ownerWallet);

        // Mark as declined
        withdrawal.setStatus(WithdrawalStatus.DECLINE);
        withdrawal.setApprovedAt(LocalDateTime.now());
        withdrawal.setApprovedBy(admin.getEmail());

        Withdrawal saved = withdrawalRepository.save(withdrawal);

        log.info("[Withdrawal] Admin {} rejected withdrawal id={} for user={}, amount={} — funds refunded",
                admin.getEmail(), withdrawalId, owner.getId(), withdrawal.getAmount());

        // Notify user via Kafka
        publishWithdrawalNotification(owner,
                "Withdrawal Rejected ❌",
                "Your withdrawal request of $" + withdrawal.getAmount() +
                " has been rejected. The amount has been refunded to your wallet.",
                "WITHDRAWAL_REJECTED");

        return saved;
    }

    // ──────────────────────────────────────────────
    // LIST QUERIES
    // ──────────────────────────────────────────────

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory(User user) {
        return withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        // PENDING first, then by date desc
        return withdrawalRepository.findAllOrderByPendingFirst();
    }

    // ──────────────────────────────────────────────
    // LEGACY (kept for backward compatibility)
    // ──────────────────────────────────────────────

    @Override
    @Deprecated
    @Transactional
    public Withdrawal proceedWithdrawal(Long withdrawalId, boolean accept) {
        Withdrawal withdrawal = getWithdrawal(withdrawalId);
        withdrawal.setDate(LocalDateTime.now());
        if (accept) {
            withdrawal.setStatus(WithdrawalStatus.SUCCESS);
        } else {
            // BUG FIX: was incorrectly setting PENDING on reject
            withdrawal.setStatus(WithdrawalStatus.DECLINE);
        }
        return withdrawalRepository.save(withdrawal);
    }

    // ──────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────

    private Withdrawal getWithdrawal(Long id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found with id " + id));
    }

    private void assertAdmin(User user) {
        if (user.getRole() != USER_ROLE.ROLE_ADMIN) {
            throw new UnauthorizedAccessException("Only admins can perform this action");
        }
    }

    private void assertPending(Withdrawal withdrawal) {
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new CustomException(
                "Withdrawal id=" + withdrawal.getId() + " is already " + withdrawal.getStatus() +
                " — only PENDING requests can be approved or rejected.",
                "WITHDRAWAL_ALREADY_PROCESSED");
        }
    }

    private void publishWithdrawalNotification(User user, String subject, String body, String type) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .type(type)
                    .subject(subject)
                    .body("<div style=\"font-family:sans-serif;padding:20px\">" + body + "</div>")
                    .build();
            notificationProducer.publish(event);
        } catch (Exception e) {
            // Non-critical — don't fail the transaction if notification publishing fails
            log.warn("[Withdrawal] Failed to publish notification for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
