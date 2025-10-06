package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order , Long> {
    Order findByUserId(Long userId);

}
