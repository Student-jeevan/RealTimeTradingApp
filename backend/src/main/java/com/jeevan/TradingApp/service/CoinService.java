package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Coin;

import java.util.List;

import org.springframework.data.domain.Page;

public interface CoinService {
    Page<Coin> getCoinList(int page, int size);

    String getMarketChart(String coinId, int days);

    String getCoinDetails(String coinId);

    Coin findById(String coinId);

    java.util.List<Coin> searchCoin(String keyword);

    String getTop50CoinsByMarketCapRank();

    String getTreadingCoins();
}
