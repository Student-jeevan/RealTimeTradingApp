package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Coin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinRepository extends JpaRepository<Coin, String> {

}
