package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.domain.PaymentMethod;
import com.jeevan.TradingApp.modal.PaymentOrder;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.response.PaymentResponse;
import com.jeevan.TradingApp.service.PaymentService;
import com.jeevan.TradingApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3001" }, allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH,
        RequestMethod.OPTIONS })
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/api/payment/{paymentMethod}/amount/{amount}")
    public ResponseEntity<PaymentResponse> paymentHandler(@PathVariable PaymentMethod paymentMethod,
            @PathVariable Long amount, @RequestHeader("Authorization") String jwt) throws Exception {
        logger.info("Received payment request: method={}, amount={}", paymentMethod, amount);

        try {
            User user = userService.findUserProfileByJwt(jwt);
            logger.info("User authenticated: {}", user.getEmail());

            PaymentResponse paymentResponse;

            PaymentOrder order = paymentService.createOrder(user, amount, paymentMethod);
            logger.info("Payment order created: orderId={}", order.getId());

            if (paymentMethod.equals(PaymentMethod.RAZORPAY)) {
                paymentResponse = paymentService.createRazorpayPaymentLink(user, amount, order.getId());
                logger.info("Razorpay payment link created");
            } else {
                paymentResponse = paymentService.createStripePaymentLink(user, amount, order.getId());
                logger.info("Stripe payment link created");
            }
            return new ResponseEntity<>(paymentResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error processing payment request: ", e);
            throw e;
        }
    }
}
