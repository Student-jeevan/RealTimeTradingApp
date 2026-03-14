package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.modal.Order;
import com.jeevan.TradingApp.exception.InsufficientBalanceException;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
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

    private void populateAvailableBalance(Wallet wallet) {
        if (wallet == null) {
            return;
        }
        java.math.BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal locked = wallet.getLockedBalance() != null ? wallet.getLockedBalance()
                : java.math.BigDecimal.ZERO;
        wallet.setAvailableBalance(balance.subtract(locked));
    }

    @Override
    public Wallet getUserWallet(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setLockedBalance(BigDecimal.ZERO);
            wallet = walletRepository.save(wallet);
        }
        populateAvailableBalance(wallet);
        return wallet;
    }

    @Autowired
    private com.jeevan.TradingApp.repository.UserRepository userRepository;

    @Override
    @Transactional
    public Wallet addBalance(User user, BigDecimal money) {
        System.out.println("WalletService.addBalance started: user=" + user.getEmail() + ", amount=" + money);
        // Fetch User and Wallet to ensure they are managed in the current transaction
        User managedUser = userRepository.findById(user.getId()).orElse(user);
        Wallet wallet = getUserWallet(managedUser);

        System.out.println("Wallet before update: balance=" + wallet.getBalance() + ", id=" + wallet.getId());

        // Create CREDIT entry in the ledger with the managed user
        ledgerService.createLedgerEntry(managedUser, LedgerTransactionType.CREDIT, money, null, "Deposit funds");

        // Update the managed wallet balance
        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(currentBalance.add(money));

        Wallet savedWallet = walletRepository.save(wallet);
        walletRepository.flush(); // Force flush to see errors immediately
        populateAvailableBalance(savedWallet);
        System.out.println("WalletService.addBalance completed: new balance=" + savedWallet.getBalance());
        return savedWallet;
    }

    @Override
    public Wallet findWalletById(Long id) {
        Optional<Wallet> wallet = walletRepository.findById(id);
        if (wallet.isPresent()) {
            return wallet.get();
        }
        throw new ResourceNotFoundException("Wallet not found for id " + id);
    }

    @Override
    @Transactional
    public Wallet walletToWalletTransfer(User sender, Wallet receiverWallet, Long amount) {
        getUserWallet(sender);
        BigDecimal amountDecimal = BigDecimal.valueOf(amount);

        // Check sufficient balance calculating against actual available amount
        BigDecimal senderAvailable = ledgerService.calculateAvailableBalance(sender.getId());
        if (senderAvailable.compareTo(amountDecimal) < 0) {
            throw new InsufficientBalanceException("Wallet balance is insufficient to transfer " + amountDecimal);
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
    public Wallet payOrderPayment(Order order, User user) {
        Wallet wallet = getUserWallet(user);
        BigDecimal orderPrice = order.getPrice();

        if (order.getOrderType().equals(OrderType.BUY)) {
            BigDecimal availableBalance = ledgerService.calculateAvailableBalance(user.getId());

            if (availableBalance.compareTo(orderPrice) < 0) {
                throw new InsufficientBalanceException("Wallet balance is insufficient to execute this trade");
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

    @Override
    @Transactional
    public Wallet debit(User user, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getUserWallet(user);
        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

        // Ensure balance doesn't go negative if possible, though higher level logic
        // (risk validation) should handle this.
        wallet.setBalance(currentBalance.subtract(amount));

        ledgerService.createLedgerEntry(user, LedgerTransactionType.DEBIT, amount, referenceId, description);

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet credit(User user, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getUserWallet(user);
        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

        wallet.setBalance(currentBalance.add(amount));

        ledgerService.createLedgerEntry(user, LedgerTransactionType.CREDIT, amount, referenceId, description);

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet releaseLock(User user, BigDecimal amount, String referenceId, String description) {
        Wallet wallet = getUserWallet(user);
        BigDecimal currentLocked = wallet.getLockedBalance() != null ? wallet.getLockedBalance() : BigDecimal.ZERO;

        wallet.setLockedBalance(currentLocked.subtract(amount));

        ledgerService.createLedgerEntry(user, LedgerTransactionType.TRADE_RELEASE, amount, referenceId, description);

        return walletRepository.save(wallet);
    }
}
