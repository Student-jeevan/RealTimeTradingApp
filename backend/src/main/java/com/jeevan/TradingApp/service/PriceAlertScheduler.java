package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.repository.CoinRepository;
import com.jeevan.TradingApp.repository.PriceAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class PriceAlertScheduler {

    @Autowired
    private PriceAlertRepository alertRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private PriceAlertService priceAlertService;

    @Scheduled(fixedDelay = 10000)
    public void checkAlerts() {
        try {
            List<PriceAlert> activeAlerts = alertRepository.findAllByTriggeredFalse();
            for (PriceAlert alert : activeAlerts) {
                try {
                    Optional<Coin> coinOpt = coinRepository.findById(alert.getCoin());
                    if (coinOpt.isEmpty())
                        continue;

                    BigDecimal currentPrice = BigDecimal.valueOf(coinOpt.get().getCurrentPrice());
                    boolean met = false;
                    switch (alert.getAlertCondition()) {
                        case ABOVE -> met = currentPrice.compareTo(alert.getTargetPrice()) >= 0;
                        case BELOW -> met = currentPrice.compareTo(alert.getTargetPrice()) <= 0;
                    }

                    if (met) {
                        priceAlertService.triggerAlert(alert, currentPrice);
                    }
                } catch (Exception e) {
                    System.err.println(
                            "[PriceAlertScheduler] Error processing alert " + alert.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[PriceAlertScheduler] Skipping check: " + e.getMessage());
        }
    }
}
