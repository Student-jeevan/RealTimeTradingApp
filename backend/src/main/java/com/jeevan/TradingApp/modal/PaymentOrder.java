package com.jeevan.TradingApp.modal;

import com.jeevan.TradingApp.domain.PaymentMethod;
import com.jeevan.TradingApp.domain.PaymentOrderStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_order_status")
    private PaymentOrderStatus paymentOrderStatus;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @ManyToOne
    private User user;
}
