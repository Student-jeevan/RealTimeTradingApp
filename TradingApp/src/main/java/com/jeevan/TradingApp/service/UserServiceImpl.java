package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository userRepository;
    @Override
    public User findUserProfileByJwt(String jwt) {
        return null;
    }
    @Override
    public User findUserByEmail(String email) {
        return null;
    }

    @Override
    public User findUserById(Long userId) {
        return null;
    }

    @Override
    public User enableTwoFactorAuthentication(User user) {
        return null;
    }

    @Override
    public User updatePassword(User user, String newPassword) {
        return null;
    }
}
