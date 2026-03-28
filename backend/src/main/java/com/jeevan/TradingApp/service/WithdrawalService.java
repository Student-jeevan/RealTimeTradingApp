package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Withdrawal;

import java.util.List;

public interface WithdrawalService {

    /** User: request a withdrawal — funds locked, status = PENDING */
    Withdrawal requestWithdrawal(Long amount, User user);

    /**
     * Legacy admin method (kept for backward compatibility).
     * @deprecated Use {@link #approveWithdrawal(Long, User)} or {@link #rejectWithdrawal(Long, User)} instead.
     */
    @Deprecated
    Withdrawal proceedWithdrawal(Long withdrawalId, boolean accept);

    /** Admin: approve a PENDING withdrawal — status → SUCCESS. Throws if not PENDING. */
    Withdrawal approveWithdrawal(Long withdrawalId, User admin);

    /** Admin: reject a PENDING withdrawal — status → DECLINE, refunds user. Throws if not PENDING. */
    Withdrawal rejectWithdrawal(Long withdrawalId, User admin);

    /** User: get own withdrawal history */
    List<Withdrawal> getUsersWithdrawalHistory(User user);

    /** Admin: get all withdrawal requests (PENDING first) */
    List<Withdrawal> getAllWithdrawalRequest();
}
