package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Watchlist;
import com.jeevan.TradingApp.repository.WatchlistRepository;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WatchlistServiceImpl implements WatchlistService {
    @Autowired
    private WatchlistRepository watchlistRepository;

    @Override
    @Cacheable(value = "userWatchlist", key = "#userId")
    public Watchlist findUserWatchlist(Long userId) {
        Watchlist watchlist = watchlistRepository.findByUserId(userId);
        if (watchlist == null) {
            throw new ResourceNotFoundException("Watchlist not found for user");
        }
        return watchlist;
    }

    @Override
    public Watchlist createWatchlist(User user) {
        Watchlist watchlist = new Watchlist();
        watchlist.setUser(user);
        return watchlistRepository.save(watchlist);
    }

    @Override
    public Watchlist findById(Long Id) {
        Optional<Watchlist> watchlistOptional = watchlistRepository.findById(Id);
        if (watchlistOptional.isEmpty()) {
            throw new ResourceNotFoundException("Watchlist not found for id " + Id);
        }
        return watchlistOptional.get();
    }

    @Override
    public Coin addItemToWatchlist(Coin coin, User user) {
        Watchlist watchlist = findUserWatchlist(user.getId());

        if (watchlist.getCoins().contains(coin)) {
            watchlist.getCoins().remove(coin);
        } else
            watchlist.getCoins().add(coin);
        watchlistRepository.save(watchlist);
        return coin;
    }
}
