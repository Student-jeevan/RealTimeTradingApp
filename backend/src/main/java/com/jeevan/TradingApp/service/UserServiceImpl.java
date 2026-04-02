package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.config.JwtProvider;
import com.jeevan.TradingApp.domain.VerificationType;
import com.jeevan.TradingApp.modal.TwoFactorAuth;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import com.jeevan.TradingApp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User findUserProfileByJwt(String jwt) {
        String email = JwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found for token subject " + email);
        }
        return user;
    }

    @Override
    public User findUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email " + email);
        }
        return user;
    }

    @Override
    public User findUserById(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }
        return user.get();
    }

    @Override
    public User enableTwoFactorAuthentication(VerificationType verificationType, String sendTo, User user) {
        TwoFactorAuth twoFactorAuth = new TwoFactorAuth();
        twoFactorAuth.setEnabled(true);
        twoFactorAuth.setSendTo(verificationType);
        user.setTwoFactorAuth(twoFactorAuth);
        return userRepository.save(user);
    }

    @Override
    public User updateTwoFactorAuthStatus(User user, boolean enabled) {
        TwoFactorAuth twoFactorAuth = user.getTwoFactorAuth();
        twoFactorAuth.setEnabled(enabled);
        twoFactorAuth.setSendTo(enabled ? twoFactorAuth.getSendTo() : null);
        user.setTwoFactorAuth(twoFactorAuth);
        return userRepository.save(user);
    }

    @Override
    public User updatePassword(User user, String newPassword) {
        user.setPassword(newPassword);
        return userRepository.save(user);
    }
}
