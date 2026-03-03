package com.jeevan.TradingApp.modal;

import com.jeevan.TradingApp.domain.LedgerTransactionType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "wallet_ledger")
public class WalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private LedgerTransactionType transactionType;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal amount;

    @Column(name = "reference_id")
    private String referenceId; // order_id or payment_id

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
