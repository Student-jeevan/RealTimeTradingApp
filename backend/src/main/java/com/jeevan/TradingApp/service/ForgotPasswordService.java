package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.VerificationType;
import com.jeevan.TradingApp.modal.ForgotPasswordToken;
import com.jeevan.TradingApp.modal.User;

public interface ForgotPasswordService {
    ForgotPasswordToken createToken(User user, String id, String otp, VerificationType verificationType, String sendTo);
    ForgotPasswordToken findById(String id);
    ForgotPasswordToken findByUser(Long userId);
    void deleteToken(ForgotPasswordToken token);
}
