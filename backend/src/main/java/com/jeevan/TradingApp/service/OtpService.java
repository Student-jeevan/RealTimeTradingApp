package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.exception.CustomException;
import com.jeevan.TradingApp.utils.OtpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final String OTP_COOLDOWN_PREFIX = "otp_cooldown:";

    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final long COOLDOWN_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Generate a 6-digit OTP, store it in Redis with 5-minute expiry, and return it.
     * Also sets a 30-second resend cooldown.
     */
    public String generateAndStoreOtp(String email) {
        // Check cooldown
        String cooldownKey = OTP_COOLDOWN_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new CustomException("OTP_COOLDOWN",
                    "Please wait 30 seconds before requesting a new OTP");
        }

        String otp = OtpUtils.generateOTP();

        // Store OTP with 5-minute expiry
        String otpKey = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(otpKey, (Object) otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        // Reset attempts counter
        String attemptsKey = OTP_ATTEMPTS_PREFIX + email;
        redisTemplate.opsForValue().set(attemptsKey, (Object) Integer.valueOf(0), OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        // Set cooldown (30 seconds)
        redisTemplate.opsForValue().set(cooldownKey, true, COOLDOWN_SECONDS, TimeUnit.SECONDS);

        return otp;
    }

    /**
     * Verify the OTP for the given email. Increments attempt counter.
     * Throws if OTP is invalid, expired, or max attempts exceeded.
     */
    public boolean verifyOtp(String email, String otp) {
        String attemptsKey = OTP_ATTEMPTS_PREFIX + email;
        String otpKey = OTP_PREFIX + email;

        // Check attempts
        Object attemptsObj = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = 0;
        if (attemptsObj != null) {
            attempts = Integer.parseInt(attemptsObj.toString());
        }

        if (attempts >= MAX_ATTEMPTS) {
            clearOtp(email);
            throw new CustomException("OTP_MAX_ATTEMPTS",
                    "Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Increment attempts
        redisTemplate.opsForValue().increment(attemptsKey);

        // Get stored OTP
        Object storedOtpObj = redisTemplate.opsForValue().get(otpKey);
        if (storedOtpObj == null) {
            throw new CustomException("OTP_EXPIRED", "OTP has expired. Please request a new one.");
        }

        String storedOtp = storedOtpObj.toString();
        if (!storedOtp.equals(otp)) {
            int remaining = MAX_ATTEMPTS - attempts - 1;
            throw new CustomException("INVALID_OTP",
                    "Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        // OTP verified — clean up
        clearOtp(email);
        return true;
    }

    /**
     * Check if OTP can be resent (cooldown expired).
     */
    public boolean canResendOtp(String email) {
        String cooldownKey = OTP_COOLDOWN_PREFIX + email;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));
    }

    /**
     * Clear all OTP-related keys for the email.
     */
    public void clearOtp(String email) {
        redisTemplate.delete(OTP_PREFIX + email);
        redisTemplate.delete(OTP_ATTEMPTS_PREFIX + email);
        redisTemplate.delete(OTP_COOLDOWN_PREFIX + email);
    }
}
