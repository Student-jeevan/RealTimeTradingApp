package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Wallet;
import com.jeevan.TradingApp.modal.WalletTransaction;
import com.jeevan.TradingApp.service.TransactionService;
import com.jeevan.TradingApp.service.UserService;
import com.jeevan.TradingApp.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/api/transactions")
    public ResponseEntity<List<WalletTransaction>> getAllTransactions(@RequestHeader("Authorization") String jwt) throws Exception {
        logger.info("Fetching all transactions for user");
        
        User user = userService.findUserProfileByJwt(jwt);
        Wallet wallet = walletService.getUserWallet(user);

        List<WalletTransaction> transactionList = transactionService.getTransactionsByWalletId(wallet);
        
        // Sort by date descending (newest first)
        transactionList = transactionList.stream()
                .sorted(Comparator.comparing(WalletTransaction::getDate).reversed())
                .collect(Collectors.toList());
        
        logger.info("Found {} transactions for user", transactionList.size());
        return new ResponseEntity<>(transactionList, HttpStatus.OK);
    }
}
