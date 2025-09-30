package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.config.JwtProvider;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import com.jeevan.TradingApp.response.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<AuthResponse> register(@RequestBody User user) throws Exception {
        User EmailExist = userRepository.findByEmail(user.getEmail());
        if(EmailExist != null){
            throw new Exception("gmail already user by another account");
        }
        User newuser = new User();
        newuser.setFullName(user.getFullName());
        newuser.setPassword(user.getPassword());
        newuser.setEmail(user.getEmail());
        User saveduser = userRepository.save(newuser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                user.getPassword()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = JwtProvider.generateToken(auth);
        AuthResponse res = new AuthResponse();
        res.setJwt(jwt);
        res.setStatus(true);
        res.setMessage("registered success");
        return new ResponseEntity<>(res , HttpStatus.CREATED);
    }
}
