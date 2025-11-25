package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.domain.WalletTransactionType;
import com.jeevan.TradingApp.modal.*;
import com.jeevan.TradingApp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
public class WalletController {
    @Autowired
    private WalletService walletService;

    @Autowired
    OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TransactionService transactionService;
    
    @GetMapping("/api/wallet")
    public ResponseEntity<Wallet> getUserWallet(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        Wallet wallet  = walletService.getUserWallet(user);

        return new ResponseEntity<>(wallet , HttpStatus.ACCEPTED);

    }

    @PutMapping("/api/wallet/{walletId}/transfer")
    public ResponseEntity<Wallet> walletToWalletTransfer(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long walletId,
            @RequestBody WalletTransaction req
            ) throws Exception {
        User senderUser = userService.findUserProfileByJwt(jwt);
        Wallet receiverWallet = walletService.findWalletById(walletId);
        Wallet wallet = walletService.walletToWalletTransfer(senderUser, receiverWallet , req.getAmount());

        transactionService.createTransaction(
                wallet,
                WalletTransactionType.WALLET_TRANSFER,
                null,
                "Transfer to wallet "+walletId,
                req.getAmount()
        );
        transactionService.createTransaction(
                receiverWallet,
                WalletTransactionType.ADD_MONEY,
                null,
                "Transfer from wallet "+wallet.getId(),
                req.getAmount()
        );

        return new ResponseEntity<>(wallet , HttpStatus.ACCEPTED);
    }

    @PutMapping("/api/wallet/order/{orderId}/pay")
    public ResponseEntity<Wallet> payOrderPayment(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId
    ) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        Order order = orderService.getOrderById(orderId);

        Wallet wallet = walletService.payOrderPayment(order , user);
        WalletTransactionType type = order.getOrderType().equals(OrderType.BUY)
                ? WalletTransactionType.BUY_ASSET
                : WalletTransactionType.SELL_ASSET;
        transactionService.createTransaction(
                wallet,
                type,
                null,
                "Order #" + orderId,
                order.getPrice().longValue()
        );
        return new ResponseEntity<>(wallet , HttpStatus.ACCEPTED);
    }

    @PutMapping("/api/wallet/deposit")
    public ResponseEntity<Wallet> addMoneyToWallet(
            @RequestHeader("Authorization") String jwt,
            @RequestParam(name="order_id") Long orderId ,
            @RequestParam(name="payment_id") String paymentId
    ) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);


        Wallet wallet = walletService.getUserWallet(user);

        PaymentOrder order = paymentService.getPaymentOrderById(orderId);

        Boolean status = paymentService.ProceedPaymentOrder(order , paymentId);
        if(wallet.getBalance() == null){
            wallet.setBalance(BigDecimal.valueOf(0));
        }
        if(status){
            wallet = walletService.addBalance(wallet , order.getAmount());
            transactionService.createTransaction(
                    wallet,
                    WalletTransactionType.ADD_MONEY,
                    paymentId,
                    "Wallet top-up",
                    order.getAmount()
            );
        }
        return new ResponseEntity<>(wallet , HttpStatus.ACCEPTED);
    }
}
