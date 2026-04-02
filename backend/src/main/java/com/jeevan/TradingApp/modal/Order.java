package com.jeevan.TradingApp.modal;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_coin", columnList = "coin_id"),
        @Index(name = "idx_orders_price", columnList = "price"),
        @Index(name = "idx_orders_status", columnList = "status")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "coin_id")
    private Coin coin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.jeevan.TradingApp.domain.OrderType orderType;

    @Column(nullable = false)
    private BigDecimal price;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.jeevan.TradingApp.domain.OrderStatus status;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private OrderItem orderItem;

    @Column(name = "quantity")
    private Double quantity = 0.0;

    @Column(name = "filled_quantity")
    private Double filledQuantity = 0.0;

    @Column(name = "remaining_quantity")
    private Double remainingQuantity = 0.0;

    @Version
    private Long version;
}
