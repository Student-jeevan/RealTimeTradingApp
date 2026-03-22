package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.AlertCondition;
import com.jeevan.TradingApp.modal.PriceAlert;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.PriceAlertRepository;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import com.jeevan.TradingApp.exception.UnauthorizedAccessException;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PriceAlertServiceImpl implements PriceAlertService {

    @Autowired
    private PriceAlertRepository alertRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public PriceAlert createAlert(User user, String coinId, BigDecimal targetPrice, AlertCondition condition) {
        PriceAlert alert = new PriceAlert();
        alert.setUser(user);
        alert.setCoin(coinId);
        alert.setTargetPrice(targetPrice);
        alert.setAlertCondition(condition);
        alert.setTriggered(false);
        alert.setCreatedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    @Override
    public List<PriceAlert> getActiveAlerts(Long userId) {
        return alertRepository.findByUserIdAndTriggeredFalse(userId);
    }

    @Override
    public List<PriceAlert> getTriggeredAlerts(Long userId) {
        return alertRepository.findByUserIdAndTriggeredTrue(userId);
    }

    @Override
    public void deleteAlert(Long alertId, Long userId) {
        PriceAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found with id " + alertId));
        if (!alert.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this alert");
        }
        alertRepository.deleteById(alertId);
    }
    @Override
    public void triggerAlert(PriceAlert alert, BigDecimal currentPrice) {
        alert.setTriggered(true);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setTriggeredPrice(currentPrice);
        alertRepository.save(alert);

        try {
            emailService.sendPriceAlertEmail(
                    alert.getUser().getEmail(),
                    alert.getCoin(),
                    alert.getTargetPrice(),
                    currentPrice,
                    alert.getAlertCondition());
        } catch (MessagingException e) {
            System.err.println("Email fail: " + e.getMessage());
        }

        messagingTemplate.convertAndSendToUser(
                alert.getUser().getId().toString(),
                "/queue/alerts",
                alert);
    }
}
