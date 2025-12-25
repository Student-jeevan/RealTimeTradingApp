package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.PaymentDetails;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.PaymentDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentDetailsServiceImpl implements PaymentDetailsService {

    @Autowired
    private PaymentDetailsRepository paymentDetailsRepository;

    @Override
    public PaymentDetails addPaymentDetails(String accountNumber, String accountHolderName, String ifsc,
            String bankName, User user) {
        PaymentDetails paymentDetails = paymentDetailsRepository.findByUserId(user.getId());

        if (paymentDetails == null) {
            paymentDetails = new PaymentDetails();
            paymentDetails.setUser(user);
        }

        paymentDetails.setAccountNumber(accountNumber);
        paymentDetails.setBankName(bankName);
        paymentDetails.setIfsc(ifsc);
        paymentDetails.setAccountHolderName(accountHolderName);

        return paymentDetailsRepository.save(paymentDetails);
    }

    @Override
    public PaymentDetails getUsersPaymentDetails(User user) {

        return paymentDetailsRepository.findByUserId(user.getId());
    }
}
