package com.jeevan.TradingApp.modal;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "locked_balance", precision = 18, scale = 8)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Version
    private Long version;

    /**
     * Derived, non-persisted field exposed in API responses.
     * availableBalance = balance - lockedBalance
     */
    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("availableBalance")
    private BigDecimal availableBalance;
}
