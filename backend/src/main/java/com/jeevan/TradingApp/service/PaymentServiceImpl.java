package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.exception.CustomException;

import com.jeevan.TradingApp.domain.PaymentMethod;
import com.jeevan.TradingApp.domain.PaymentOrderStatus;
import com.jeevan.TradingApp.modal.PaymentOrder;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.PaymentOrderRepository;
import com.jeevan.TradingApp.response.PaymentResponse;
import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecretKey;

    @Override
    public PaymentOrder createOrder(User user, Long amount, PaymentMethod paymentMethod) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setUser(user);
        paymentOrder.setAmount(amount);
        paymentOrder.setPaymentMethod(paymentMethod);
        paymentOrder.setPaymentOrderStatus(PaymentOrderStatus.PENDING);

        return paymentOrderRepository.save(paymentOrder);
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long id) {

        return paymentOrderRepository.findById(id)
                .orElseThrow(() -> new CustomException("payment order not found", "NOT_FOUND"));
    }

    @Override
    public Boolean ProceedPaymentOrder(PaymentOrder paymentOrder, String paymentId) {
        if (paymentOrder.getPaymentOrderStatus() == null) {
            paymentOrder.setPaymentOrderStatus(PaymentOrderStatus.PENDING);
        }
        if (paymentOrder.getPaymentOrderStatus().equals(PaymentOrderStatus.PENDING)) {
            if (paymentOrder.getPaymentMethod().equals(PaymentMethod.RAZORPAY)) {
                RazorpayClient razorpay = null;
                try {
                    razorpay = new RazorpayClient(apiKey, apiSecretKey);
                } catch (RazorpayException e) {
                    throw new CustomException(e.getMessage(), "PAYMENT_SERVICE_ERROR");
                }
                Payment payment = null;
                try {
                    payment = razorpay.payments.fetch(paymentId);
                } catch (RazorpayException e) {
                    throw new CustomException(e.getMessage(), "PAYMENT_FETCH_ERROR");
                }

                Integer amount = payment.get("amount");

                String status = payment.get("status");
                System.out.println("Razorpay status for " + paymentId + ": " + status);

                if (status.equals("captured") || status.equals("authorized")) {
                    paymentOrder.setPaymentOrderStatus(PaymentOrderStatus.SUCCESS);
                    paymentOrderRepository.save(paymentOrder);
                    return true;
                } else {
                    paymentOrder.setPaymentOrderStatus(PaymentOrderStatus.FAILED);
                    paymentOrderRepository.save(paymentOrder);
                    return false;
                }
            }
            paymentOrder.setPaymentOrderStatus(PaymentOrderStatus.SUCCESS);
            paymentOrderRepository.save(paymentOrder);
            return true;
        }
        return false;
    }

    @Override
    public PaymentResponse createRazorpayPaymentLink(User user, Long amount, Long orderId) {
        // Razorpay expects amount in paise (smallest currency unit), so multiply by 100
        Long amountInPaise = amount * 100;
        try {
            RazorpayClient razorpay = new RazorpayClient(apiKey, apiSecretKey);
            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", amountInPaise);
            paymentLinkRequest.put("currency", "INR");

            JSONObject customer = new JSONObject();
            customer.put("name", user.getFullName());
            customer.put("email", user.getEmail());
            paymentLinkRequest.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("email", true);
            paymentLinkRequest.put("reminder_enable", true);

            // Fixed callback URL to point to frontend with correct query parameter format
            paymentLinkRequest.put("callback_url", "http://localhost:3001/wallet?order_id=" + orderId);
            paymentLinkRequest.put("callback_method", "get");

            PaymentLink payment = razorpay.paymentLink.create(paymentLinkRequest);
            String paymentLinkId = payment.get("id");
            String paymentLinkUrl = payment.get("short_url");
            PaymentResponse res = new PaymentResponse();
            res.setPayment_url(paymentLinkUrl);
            return res;

        } catch (RazorpayException e) {
            System.out.println("Error creating payment link : " + e.getMessage());
            throw new CustomException(e.getMessage(), "PAYMENT_LINK_ERROR");
        }
    }

    @Override
    public PaymentResponse createStripePaymentLink(User user, Long amount, Long orderId) {
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:3001/wallet?order_id=" + orderId)
                .setCancelUrl("http://localhost:3001/wallet")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amount * 100)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Top up wallet")
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = null;
        try {
            session = Session.create(params);
        } catch (StripeException e) {
            throw new CustomException(e.getMessage(), "STRIPE_ERROR");
        }
        PaymentResponse res = new PaymentResponse();
        res.setPayment_url(session.getUrl());
        return res;
    }
}
