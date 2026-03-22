package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.WalletLedger;
import com.jeevan.TradingApp.repository.WalletLedgerRepository;
import com.jeevan.TradingApp.kafka.events.TransactionEvent;
import com.jeevan.TradingApp.kafka.producer.TransactionEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class LedgerServiceImpl implements LedgerService {

    @Autowired
    private WalletLedgerRepository walletLedgerRepository;

    @Autowired
    private TransactionEventProducer transactionEventProducer;

    @Override
    @Transactional
    public WalletLedger createLedgerEntry(User user, LedgerTransactionType type, BigDecimal amount, String referenceId,
            String description) {
        WalletLedger entry = new WalletLedger();
        entry.setUser(user);
        entry.setTransactionType(type);
        entry.setAmount(amount);
        entry.setReferenceId(referenceId);
        entry.setDescription(description);
        WalletLedger saved = walletLedgerRepository.save(entry);

        // --- Publish transaction event to Kafka for audit ---
        try {
            TransactionEvent txnEvent = TransactionEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .timestamp(java.time.LocalDateTime.now())
                    .userId(user.getId())
                    .transactionType(type.name())
                    .amount(amount)
                    .referenceId(referenceId)
                    .description(description)
                    .build();
            transactionEventProducer.publish(txnEvent);
        } catch (Exception e) {
            System.err.println("[LedgerService] Failed to publish txn event: " + e.getMessage());
        }

        return saved;
    }

    @Override
    public BigDecimal calculateAvailableBalance(Long userId) {
        System.out.println("Calculating available balance for userId: " + userId);

        BigDecimal additions = walletLedgerRepository.sumAmountByUserIdAndTransactionTypes(
                userId, Arrays.asList(LedgerTransactionType.CREDIT, LedgerTransactionType.TRADE_RELEASE))
                .orElse(BigDecimal.ZERO);
        System.out.println("Additions: " + additions);

        BigDecimal subtractions = walletLedgerRepository.sumAmountByUserIdAndTransactionTypes(
                userId,
                Arrays.asList(LedgerTransactionType.DEBIT, LedgerTransactionType.TRADE_LOCK, LedgerTransactionType.FEE))
                .orElse(BigDecimal.ZERO);
        System.out.println("Subtractions: " + subtractions);

        BigDecimal balance = additions.subtract(subtractions);
        System.out.println("Calculated Available Balance: " + balance);
        return balance;
    }

    @Override
    public List<WalletLedger> getUserLedger(Long userId) {
        return walletLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
