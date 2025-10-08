package com.jeevan.TradingApp.modal;

import com.jeevan.TradingApp.domain.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Withdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long Id;
    private WithdrawalStatus status;
    private Long amount;

    @ManyToOne
    private User user;

    private LocalDateTime date = LocalDateTime.now();

}
