package com.jeevan.TradingApp.modal;

import com.jeevan.TradingApp.domain.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;

    private Long amount;

    @ManyToOne
    private User user;

    private LocalDateTime date = LocalDateTime.now();

    /** Timestamp when admin approved or rejected this request. */
    private LocalDateTime approvedAt;

    /** Email/name of the admin who acted on this request. */
    private String approvedBy;
}
