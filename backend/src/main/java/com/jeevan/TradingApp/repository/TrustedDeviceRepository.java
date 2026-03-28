package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.TrustedDevice;
import com.jeevan.TradingApp.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByUserAndDeviceId(User user, String deviceId);

    void deleteByExpiryTimeBefore(LocalDateTime now);
}
