package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder , Long> {

}
