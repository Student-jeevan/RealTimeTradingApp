package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.WithdrawalStatus;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Withdrawal;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.repository.WithdrawalRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WithdrawalServiceImpl implements WithdrawalService {
    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Override
    public Withdrawal requestWithdrawal(Long amount, User user) {
        Wallet userWallet = walletService.getUserWallet(user);

        // Check for insufficient funds
        if (userWallet.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Deduct balance immediately to lock funds
        userWallet.setBalance(userWallet.getBalance().subtract(BigDecimal.valueOf(amount)));
        walletRepository.save(userWallet);

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setAmount(amount);
        withdrawal.setUser(user);
        withdrawal.setStatus(WithdrawalStatus.PENDING);

        return withdrawalRepository.save(withdrawal);
    }

    @Override
    public Withdrawal proceedWithdrawal(Long withdrawalId, boolean accept) throws Exception {
        Optional<Withdrawal> withdrawal = withdrawalRepository.findById(withdrawalId);
        if (withdrawal.isEmpty()) {
            throw new Exception("withdrawal not found");
        }
        Withdrawal withdrawal1 = withdrawal.get();
        withdrawal1.setDate(LocalDateTime.now());
        if (accept) {
            withdrawal1.setStatus(WithdrawalStatus.SUCCESS);
        } else {
            withdrawal1.setStatus(WithdrawalStatus.PENDING);
        }
        return withdrawalRepository.save(withdrawal1);
    }

    @Override
    public List<Withdrawal> getUsersWithdrawalHistory(User user) {

        return withdrawalRepository.findByUserId(user.getId());
    }

    @Override
    public List<Withdrawal> getAllWithdrawalRequest() {
        return withdrawalRepository.findAll();
    }
}
