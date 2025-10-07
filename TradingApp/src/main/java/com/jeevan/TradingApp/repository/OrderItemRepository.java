package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}
