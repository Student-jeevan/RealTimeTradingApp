package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.TrustedDevice;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.TrustedDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DeviceService {

    private static final int TRUST_DURATION_DAYS = 30;

    @Autowired
    private TrustedDeviceRepository trustedDeviceRepository;

    /**
     * Check if the given deviceId is a trusted device for the user and has not expired.
     */
    public boolean isTrustedDevice(User user, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        Optional<TrustedDevice> device = trustedDeviceRepository.findByUserAndDeviceId(user, deviceId);
        return device.isPresent() && device.get().getExpiryTime().isAfter(LocalDateTime.now());
    }

    /**
     * Mark a device as trusted for 30 days. If the device already exists, refresh its expiry.
     */
    @Transactional
    public void trustDevice(User user, String deviceId) {
        Optional<TrustedDevice> existing = trustedDeviceRepository.findByUserAndDeviceId(user, deviceId);

        TrustedDevice device;
        if (existing.isPresent()) {
            device = existing.get();
        } else {
            device = new TrustedDevice();
            device.setUser(user);
            device.setDeviceId(deviceId);
        }
        device.setExpiryTime(LocalDateTime.now().plusDays(TRUST_DURATION_DAYS));
        trustedDeviceRepository.save(device);
    }

    /**
     * Cleanup expired trusted devices (can be called via a scheduled task).
     */
    @Transactional
    public void removeExpiredDevices() {
        trustedDeviceRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }
}
