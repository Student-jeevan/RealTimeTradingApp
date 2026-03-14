package com.jeevan.TradingApp.modal;

import com.jeevan.TradingApp.domain.WalletTransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private com.jeevan.TradingApp.domain.WalletTransactionType type;

    private LocalDate date;

    private String transferId;

    private String purpose;

    private Long amount;

}
