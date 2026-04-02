package com.jeevan.TradingApp.modal;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class VerificationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String otp;
    @OneToOne
    private User user;
    private String email;
    private String mobile;

    @Enumerated(EnumType.STRING)
    private com.jeevan.TradingApp.domain.VerificationType verificationType;
}
