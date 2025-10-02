package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.TwoFactorOTP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorOtpRepository extends JpaRepository<TwoFactorOTP , String>{
    TwoFactorOTP findByUserId(Long userId);
}
