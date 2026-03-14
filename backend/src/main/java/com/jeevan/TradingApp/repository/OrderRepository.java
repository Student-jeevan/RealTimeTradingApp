package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.domain.OrderStatus;
import com.jeevan.TradingApp.domain.OrderType;
import com.jeevan.TradingApp.modal.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.coin.id = :coinId AND o.status IN :statuses AND o.orderType = :orderType AND o.price <= :price ORDER BY o.price ASC, o.timestamp ASC")
    List<Order> getMatchingSellOrders(@Param("coinId") String coinId,
                                      @Param("price") BigDecimal price,
                                      @Param("orderType") OrderType orderType,
                                      @Param("statuses") List<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.coin.id = :coinId AND o.status IN :statuses AND o.orderType = :orderType AND o.price >= :price ORDER BY o.price DESC, o.timestamp ASC")
    List<Order> getMatchingBuyOrders(@Param("coinId") String coinId,
                                     @Param("price") BigDecimal price,
                                     @Param("orderType") OrderType orderType,
                                     @Param("statuses") List<OrderStatus> statuses);

    List<Order> findByCoinIdAndStatusOrderByTimestampDesc(String coinId, OrderStatus status);

}
