package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByUserIdAndTriggeredFalse(Long userId);

    List<PriceAlert> findByUserIdAndTriggeredTrue(Long userId);

    List<PriceAlert> findAllByTriggeredFalse();
}
