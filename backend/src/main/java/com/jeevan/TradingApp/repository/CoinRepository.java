package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CoinRepository extends JpaRepository<Coin, String> {
    @Query("SELECT c FROM Coin c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.symbol) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Coin> searchCoins(@Param("keyword") String keyword);
}
