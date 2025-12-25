package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.Watchlist;
import com.jeevan.TradingApp.repository.WalletRepository;
import com.jeevan.TradingApp.repository.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WatchlistServiceImpl implements WatchlistService {
    @Autowired
    private WatchlistRepository watchlistRepository;

    @Override
    @Cacheable(value = "userWatchlist", key = "#userId")
    public Watchlist findUserWatchlist(Long userId) throws Exception {
        Watchlist watchlist = watchlistRepository.findByUserId(userId);
        if (watchlist == null) {
            throw new Exception("watchlist not found");
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
    public Watchlist findById(Long Id) throws Exception {
        Optional<Watchlist> watchlistOptional = watchlistRepository.findById(Id);
        if (watchlistOptional.isEmpty()) {
            throw new Exception("Watchlist not found");
        }
        return watchlistOptional.get();
    }

    @Override
    public Coin addItemToWatchlist(Coin coin, User user) throws Exception {
        Watchlist watchlist = findUserWatchlist(user.getId());

        if (watchlist.getCoins().contains(coin)) {
            watchlist.getCoins().remove(coin);
        } else
            watchlist.getCoins().add(coin);
        watchlistRepository.save(watchlist);
        return coin;
    }
}
