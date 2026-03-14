package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.modal.*;
import com.jeevan.TradingApp.service.OrderService;
import com.jeevan.TradingApp.service.PaymentService;
import com.jeevan.TradingApp.service.UserService;
import com.jeevan.TradingApp.service.WalletService;
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

    @GetMapping("/api/wallet")
    public ResponseEntity<Wallet> getUserWallet(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt);

        Wallet wallet = walletService.getUserWallet(user);
        System.out.println("Fetching wallet for user: " + user.getEmail() + ", balance: " + wallet.getBalance());

        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @PutMapping("/api/wallet/{walletId}/transfer")
    public ResponseEntity<Wallet> walletToWalletTransfer(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long walletId,
            @RequestBody WalletTransaction req) {
        User senderUser = userService.findUserProfileByJwt(jwt);
        Wallet receiverWallet = walletService.findWalletById(walletId);
        Wallet wallet = walletService.walletToWalletTransfer(senderUser, receiverWallet, req.getAmount());

        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @PutMapping("/api/wallet/order/{orderId}/pay")
    public ResponseEntity<Wallet> payOrderPayment(
            @RequestHeader("Authorization") String jwt,
            @PathVariable Long orderId) {
        User user = userService.findUserProfileByJwt(jwt);
        Order order = orderService.getOrderById(orderId);

        Wallet wallet = walletService.payOrderPayment(order, user);
        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @GetMapping("/api/wallet/deposit")
    public ResponseEntity<Wallet> addMoneyToWallet(
            @RequestHeader(name = "Authorization", required = false) String jwt,
            @RequestParam(name = "order_id") Long orderId,
            @RequestParam(name = "payment_id") String paymentId) {

        System.out.println("Deposit attempt: orderId=" + orderId + ", paymentId=" + paymentId);
        PaymentOrder order = paymentService.getPaymentOrderById(orderId);
        User user;

        if (jwt != null) {
            user = userService.findUserProfileByJwt(jwt);
        } else {
            user = order.getUser();
        }
        System.out.println("Processing deposit for user: " + user.getEmail() + ", current order status: "
                + order.getPaymentOrderStatus());

        // If it's already success, just return the wallet without adding balance again
        if (order.getPaymentOrderStatus().equals(com.jeevan.TradingApp.domain.PaymentOrderStatus.SUCCESS)) {
            System.out.println("Order already processed successfully. Returning wallet.");
            Wallet wallet = walletService.getUserWallet(user);
            return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
        }

        Boolean status = paymentService.ProceedPaymentOrder(order, paymentId);
        System.out.println("Payment verification status: " + status);

        Wallet wallet = walletService.getUserWallet(user);

        if (status) {
            BigDecimal inrAmount = BigDecimal.valueOf(order.getAmount());
            BigDecimal usdAmount = inrAmount.divide(BigDecimal.valueOf(83), 2, java.math.RoundingMode.HALF_UP);
            System.out.println("Adding balance: " + usdAmount + " USD (from " + inrAmount + " INR)");
            wallet = walletService.addBalance(user, usdAmount);
        }
        return new ResponseEntity<>(wallet, HttpStatus.ACCEPTED);
    }

    @Autowired
    private com.jeevan.TradingApp.service.LedgerService ledgerService;

    @GetMapping("/api/wallet/ledger")
    public ResponseEntity<java.util.List<WalletLedger>> getUserLedger(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt);
        java.util.List<WalletLedger> ledger = ledgerService.getUserLedger(user.getId());
        return new ResponseEntity<>(ledger, HttpStatus.OK);
    }
}
