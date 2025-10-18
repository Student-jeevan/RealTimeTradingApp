package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.VerificationType;
import com.jeevan.TradingApp.modal.ForgotPasswordToken;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.ForgotPasswordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ForgotPasswordServiceImpl implements ForgotPasswordService{

    @Autowired
    private ForgotPasswordRepository forgotPasswordRepository;

    @Override
    public ForgotPasswordToken createToken(User user, String id, String otp, VerificationType verificationType, String sendTo) {
        ForgotPasswordToken token = new ForgotPasswordToken();
        token.setUser(user);
        token.setOtp(otp);
        token.setSendTo(sendTo);
        token.setId(id);
        token.setVerificationType(verificationType);
        return forgotPasswordRepository.save(token);
    }

    @Override
    public ForgotPasswordToken findById(String id) {
        Optional<ForgotPasswordToken> token = forgotPasswordRepository.findById(id);
        return token.orElse(null);
    }

    @Override
    public ForgotPasswordToken findByUser(Long userId) {
        return forgotPasswordRepository.findByUserId(userId);
    }
    @Override
    public void deleteToken(ForgotPasswordToken token) {
        forgotPasswordRepository.delete(token);
    }
}
