package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.PaymentMethod;
import com.jeevan.TradingApp.modal.PaymentOrder;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.response.PaymentResponse;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;

public interface PaymentService {
    PaymentOrder createOrder(User user , Long amount , PaymentMethod paymentMethod);

    PaymentOrder getPaymentOrderById(Long id) throws Exception;

    Boolean ProceedPaymentOrder(PaymentOrder paymentOrder , String paymentId) throws RazorpayException;

    PaymentResponse createRazorpayPaymentLink(User user , Long amount , Long orderId) throws RazorpayException;

    PaymentResponse createStripePaymentLink(User user , Long amount , Long orderId) throws StripeException;
}
