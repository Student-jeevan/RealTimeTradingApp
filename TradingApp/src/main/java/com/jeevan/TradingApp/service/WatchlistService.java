package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Watchlist;

public interface WatchlistService {

    Watchlist findUserWatchlist(Long userId) throws Exception;

    Watchlist createWatchlist(User user);

    Watchlist findById(Long Id) throws Exception;

    Coin addItemToWatchlist(Coin coin , User user) throws Exception;

}
