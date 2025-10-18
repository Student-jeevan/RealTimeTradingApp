package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode , Long> {
    public VerificationCode  findByUserId(Long userId);
}
