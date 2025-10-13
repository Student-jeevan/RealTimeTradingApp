package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.PaymentDetails;
import com.jeevan.TradingApp.modal.User;

public interface PaymentDetailsService {

    public PaymentDetails addPaymentDetails(String accountNumber , String accountHolderName , String ifsc , String bankName , User user);
    public PaymentDetails getUsersPaymentDetails(User user);
}
