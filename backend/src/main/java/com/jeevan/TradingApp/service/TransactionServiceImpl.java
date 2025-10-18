package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.WalletTransactionType;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.WalletTransaction;
import com.jeevan.TradingApp.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
public class TransactionServiceImpl implements  TransactionService{
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Override
    public List<WalletTransaction> getTransactionsByWalletId(Wallet wallet) {
        return walletTransactionRepository.findByWalletId(wallet.getId());
    }
    @Override
    public WalletTransaction createTransaction( Wallet wallet,  WalletTransactionType type, String transferId, String purpose, Long amount) {
        if (wallet == null || wallet.getId() == null) {
            throw new IllegalArgumentException("Wallet must not be null or unsaved");
        }

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setDate(LocalDate.now());
        transaction.setPurpose(purpose);
        transaction.setAmount(amount);

        return walletTransactionRepository.save(transaction);
    }
}
