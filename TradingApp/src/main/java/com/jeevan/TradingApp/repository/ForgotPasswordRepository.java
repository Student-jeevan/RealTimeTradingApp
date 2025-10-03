package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.ForgotPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForgotPasswordRepository extends JpaRepository<ForgotPasswordToken , String> {
    ForgotPasswordToken findByUserId(Long id);
}
