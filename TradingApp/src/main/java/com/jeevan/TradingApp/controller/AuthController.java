package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    UserRepository userRepository;
    @PostMapping("/signup")
    public ResponseEntity<User> register(@RequestBody User user){
        User newuser = new User();
        newuser.setFullName(user.getFullName());
        newuser.setPassword(user.getPassword());
        newuser.setEmail(user.getEmail());
        User saveduser = userRepository.save(newuser);
        return new ResponseEntity<>(saveduser , HttpStatus.CREATED);
    }
}
