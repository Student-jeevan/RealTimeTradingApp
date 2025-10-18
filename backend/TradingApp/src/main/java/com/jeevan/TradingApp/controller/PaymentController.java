package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.domain.PaymentMethod;
import com.jeevan.TradingApp.modal.PaymentOrder;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.response.PaymentResponse;
import com.jeevan.TradingApp.service.PaymentService;
import com.jeevan.TradingApp.service.UserService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/api/payment/{paymentMethod}/amount/{amount}")
    public ResponseEntity<PaymentResponse> paymentHandler(@PathVariable PaymentMethod paymentMethod , @PathVariable Long amount , @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        PaymentResponse paymentResponse;

        PaymentOrder order = paymentService.createOrder(user , amount , paymentMethod);

        if(paymentMethod.equals(PaymentMethod.RAZORPAY)){
            paymentResponse = paymentService.createRazorpayPaymentLink(user , amount);
        }
        else{
            paymentResponse  = paymentService.createStripePaymentLink(user , amount , order.getId());
        }
        return new ResponseEntity<>(paymentResponse , HttpStatus.CREATED);
    }
}
