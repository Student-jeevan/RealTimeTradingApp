package com.jeevan.TradingApp.domain;

public enum WithdrawalStatus {
    /** Request received from user, awaiting admin decision */
    PENDING,
    /** Admin approved — funds released */
    SUCCESS,
    /** Admin approved (alias for SUCCESS, for new API responses) */
    APPROVED,
    /** Admin declined — funds refunded to user wallet */
    DECLINE,
    /** Admin rejected (alias for DECLINE, for new API responses) */
    REJECTED
}
