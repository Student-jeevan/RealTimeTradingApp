package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.VerificationType;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.VerificationCode;

public interface VerificationCodeService {
    VerificationCode sendVerificationCode(User user , VerificationType verificationType);
    VerificationCode getVerificationCodeById(Long Id) throws Exception;
    VerificationCode getVerificationCodeByUser(Long userId);
    void deleteVerificationCodeById(VerificationCode verificationCode);
}
