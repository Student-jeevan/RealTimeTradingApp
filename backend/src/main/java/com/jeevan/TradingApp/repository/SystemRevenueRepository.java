package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.SystemRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemRevenueRepository extends JpaRepository<SystemRevenue, Long> {
}
