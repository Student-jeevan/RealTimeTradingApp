package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist , Long> {
    Watchlist findByUserId(Long userId);
}
