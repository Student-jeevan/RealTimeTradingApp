package com.jeevan.TradingApp.modal;

import com.jeevan.TradingApp.domain.AlertCondition;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "price_alert")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String coin;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_condition", nullable = false)
    private AlertCondition alertCondition;

    @Column(nullable = false)
    private boolean triggered = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime triggeredAt;

    @Column(precision = 30, scale = 8)
    private BigDecimal triggeredPrice;
}
