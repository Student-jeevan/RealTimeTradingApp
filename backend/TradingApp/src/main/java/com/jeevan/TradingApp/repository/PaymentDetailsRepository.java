package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.PaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailsRepository extends JpaRepository<PaymentDetails , Long> {

    PaymentDetails findByUserId(Long userId);
}
