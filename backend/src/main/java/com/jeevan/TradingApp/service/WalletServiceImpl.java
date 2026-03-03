package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.modal.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private FeeService feeService;

    @Override
    public Wallet getUserWallet(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUser(user);
            walletRepository.save(wallet);
        }

        // Calculate dynamic balance from ledger
        BigDecimal availableBalance = ledgerService.calculateAvailableBalance(user.getId());

        // Synchronize wallet record with calculated balance
        wallet.setBalance(
                availableBalance.add(wallet.getLockedBalance() != null ? wallet.getLockedBalance() : BigDecimal.ZERO));
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet addBalance(Wallet wallet, BigDecimal money) {
        // Create CREDIT entry in the ledger
        ledgerService.createLedgerEntry(wallet.getUser(), LedgerTransactionType.CREDIT, money, null, "Deposit funds");

        // Return updated wallet
        return getUserWallet(wallet.getUser());
    }

    @Override
    public Wallet findWalletById(Long id) throws Exception {
        Optional<Wallet> wallet = walletRepository.findById(id);
        if (wallet.isPresent()) {
            return wallet.get();
        }
        throw new Exception("wallet not found");
    }

    @Override
    @Transactional
    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, Long amount) throws Exception {
        Wallet senderWallet = getUserWallet(sender);
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);

        // Check sufficient balance calculating against actual available amount
        BigDecimal senderAvailable = ledgerService.calculateAvailableBalance(sender.getId());
        if (senderAvailable.compareTo(amountDecimal) < 0) {
            throw new Exception("Insufficient balance ...");
        }

        // Deduct from sender via Ledger
        ledgerService.createLedgerEntry(sender, LedgerTransactionType.DEBIT, amountDecimal, null,
                "Transfer to wallet " + receiverWallet.getId());

        // Add to receiver via Ledger
        ledgerService.createLedgerEntry(receiverWallet.getUser(), LedgerTransactionType.CREDIT, amountDecimal, null,
                "Transfer from wallet " + sender.getId());

        // Refresh sender wallet wrapper
        return getUserWallet(sender);
    }

    @Override
    @Transactional
    public Wallet payOrderPayment(Order order, User user) throws Exception {
        Wallet wallet = getUserWallet(user);
        BigDecimal orderPrice = order.getPrice();

        if (order.getOrderType().equals(OrderType.BUY)) {
            BigDecimal availableBalance = ledgerService.calculateAvailableBalance(user.getId());

            if (availableBalance.compareTo(orderPrice) < 0) {
                throw new Exception("Insufficient funds for this transaction");
            }

            // On a new BUY order, we lock the funds.
            ledgerService.createLedgerEntry(user, LedgerTransactionType.TRADE_LOCK, orderPrice,
                    String.valueOf(order.getId()), "Trade lock for BUY order");

            // Update wallet's locked balance manually to reflect in entity quickly if
            // needed
            BigDecimal currentLocked = wallet.getLockedBalance() != null ? wallet.getLockedBalance() : BigDecimal.ZERO;
            wallet.setLockedBalance(currentLocked.add(orderPrice));
            walletRepository.save(wallet);

        } else {
            // For SELL orders, we might not lock fiat, assuming crypto asset validation
            // handles the sell availability.
            // If the order executes successfully, we just grant the funds (DEBIT mapping
            // occurs at execution phase in OrderService).
            // Currently this method handles both creation and execution implicitly in the
            // old flow. In the upgraded version, `payOrderPayment` could be mapped to Lock
            // phase.
            // In the context of Order execution for SELL, funds are credited. Let's just
            // create a CREDIT for a SELL complete here temporarily if that's what was
            // intended. Actually in new system, OrderService handles this explicitly.
        }

        // Let's simply return updated wallet
        return getUserWallet(user);
    }
}
