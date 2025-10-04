package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Coin;

import java.util.List;

public interface CoinService {
    List<Coin> getCoinList(int page);

    String getMarketChart(String coinId , int days);

    String getCoinDetails(String coinId);

    Coin findById(String coinId);

    String searchCoin(String keyword);

    String getTop50CoinsByMarketCapRank();

    String GetTradingCoins();
}
