package com.jeevan.TradingApp.repository;

import com.jeevan.TradingApp.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
