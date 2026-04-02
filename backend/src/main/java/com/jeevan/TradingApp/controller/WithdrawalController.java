package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.domain.USER_ROLE;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.Withdrawal;
import com.jeevan.TradingApp.service.TransactionService;
import com.jeevan.TradingApp.service.UserService;
import com.jeevan.TradingApp.service.WalletService;
import com.jeevan.TradingApp.service.WithdrawalService;
import com.jeevan.TradingApp.domain.WalletTransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WithdrawalController {

    @Autowired private WithdrawalService withdrawalService;
    @Autowired private WalletService walletService;
    @Autowired private UserService userService;
    @Autowired private TransactionService transactionService;

    // ──────────────────────────────────────────────
    // USER ENDPOINTS
    // ──────────────────────────────────────────────

    /**
     * POST /api/withdrawal/{amount}
     * User requests a withdrawal. Funds locked, status = PENDING.
     */
    @PostMapping("/api/withdrawal/{amount}")
    public ResponseEntity<?> withdrawalRequest(
            @PathVariable Long amount,
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        Wallet userWallet = walletService.getUserWallet(user);

        Withdrawal withdrawal = withdrawalService.requestWithdrawal(amount, user);

        // Record wallet transaction for ledger
        transactionService.createTransaction(
                userWallet,
                WalletTransactionType.WITHDRAWAL,
                null,
                "Withdrawal request — awaiting admin approval",
                withdrawal.getAmount());

        return ResponseEntity.ok(withdrawal);
    }

    /**
     * GET /api/withdrawal
     * User: get own withdrawal history.
     */
    @GetMapping("/api/withdrawal")
    public ResponseEntity<List<Withdrawal>> getWithdrawalHistory(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        return ResponseEntity.ok(withdrawalService.getUsersWithdrawalHistory(user));
    }

    // ──────────────────────────────────────────────
    // ADMIN ENDPOINTS
    // ──────────────────────────────────────────────

    /**
     * GET /admin/withdrawals
     * Admin: get all withdrawal requests (PENDING first).
     */
    @GetMapping("/admin/withdrawals")
    public ResponseEntity<List<Withdrawal>> getAllWithdrawals(
            @RequestHeader("Authorization") String jwt) {

        getAdminUser(jwt);
        return ResponseEntity.ok(withdrawalService.getAllWithdrawalRequest());
    }

    /**
     * POST /admin/withdrawals/{id}/approve
     * Admin: approve a PENDING withdrawal.
     */
    @PostMapping("/admin/withdrawals/{id}/approve")
    public ResponseEntity<?> approveWithdrawal(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) {

        User admin = getAdminUser(jwt);
        Withdrawal withdrawal = withdrawalService.approveWithdrawal(id, admin);
        return ResponseEntity.ok(Map.of(
                "message", "Withdrawal approved successfully",
                "withdrawal", withdrawal
        ));
    }

    /**
     * POST /admin/withdrawals/{id}/reject
     * Admin: reject a PENDING withdrawal — funds refunded to user.
     */
    @PostMapping("/admin/withdrawals/{id}/reject")
    public ResponseEntity<?> rejectWithdrawal(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) {

        User admin = getAdminUser(jwt);
        Withdrawal withdrawal = withdrawalService.rejectWithdrawal(id, admin);
        return ResponseEntity.ok(Map.of(
                "message", "Withdrawal rejected — amount refunded to user wallet",
                "withdrawal", withdrawal
        ));
    }

    // ──────────────────────────────────────────────
    // LEGACY (kept for backward compatibility)
    // ──────────────────────────────────────────────

    /**
     * @deprecated Use POST /admin/withdrawals/{id}/approve or /reject instead.
     */
    @PatchMapping("/api/admin/withdrawal/{id}/proceed/{accept}")
    @Deprecated
    public ResponseEntity<?> proceedWithdrawal(
            @PathVariable Long id,
            @PathVariable boolean accept,
            @RequestHeader("Authorization") String jwt) {

        User user = getAdminUser(jwt);
        if (accept) {
            return ResponseEntity.ok(withdrawalService.approveWithdrawal(id, user));
        } else {
            return ResponseEntity.ok(withdrawalService.rejectWithdrawal(id, user));
        }
    }

    /**
     * @deprecated Use GET /admin/withdrawals instead.
     */
    @GetMapping("/api/admin/withdrawal")
    @Deprecated
    public ResponseEntity<List<Withdrawal>> getAllWithdrawalRequestLegacy(
            @RequestHeader("Authorization") String jwt) {

        getAdminUser(jwt);
        return ResponseEntity.ok(withdrawalService.getAllWithdrawalRequest());
    }

    // ──────────────────────────────────────────────
    // HELPER
    // ──────────────────────────────────────────────

    private User getAdminUser(String jwt) {
        User user = userService.findUserProfileByJwt(jwt);
        if (user.getRole() != USER_ROLE.ROLE_ADMIN) {
            throw new UnauthorizedAccessException("Admin access required");
        }
        return user;
    }
}
