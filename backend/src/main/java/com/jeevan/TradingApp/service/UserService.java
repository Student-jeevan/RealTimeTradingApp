package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.VerificationType;
import com.jeevan.TradingApp.modal.User;

public interface UserService {
    public User findUserProfileByJwt(String jwt);

    public User findUserByEmail(String email);

    public User findUserById(Long userId);

    public User enableTwoFactorAuthentication(VerificationType verificationType, String sendTo, User user);

    User updateTwoFactorAuthStatus(User user, boolean enabled);

    User updatePassword(User user, String newPassword);
}
