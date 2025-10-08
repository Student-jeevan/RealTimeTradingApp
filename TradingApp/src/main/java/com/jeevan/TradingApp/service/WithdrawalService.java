package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Withdrawal;

import java.util.List;

public interface WithdrawalService {
    Withdrawal requestWithdrawal(Long amount , User user);
    Withdrawal proceedWithdrawal(Long withdrawalId , boolean  accept) throws Exception;
    List<Withdrawal> getUsersWithdrawalHistory(User user);
    List<Withdrawal> getAllWithdrawalRequest();

}
