package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.TwoFactorOTP;
import com.jeevan.TradingApp.modal.User;

public interface TwoFactorOtpService {
    TwoFactorOTP createTwoFactorOtp(User user , String otp , String jwt);
    TwoFactorOTP findByUser(Long userId);
    TwoFactorOTP findById(String Id);
    boolean verifyTwoFactorOtp(TwoFactorOTP twoFactorOTP , String otp);
    void deleteTwoFactorOtp(TwoFactorOTP twoFactorOTP);
}
