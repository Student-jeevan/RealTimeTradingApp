package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.config.JwtProvider;
import com.jeevan.TradingApp.modal.TwoFactorOTP;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import com.jeevan.TradingApp.response.AuthResponse;
import com.jeevan.TradingApp.service.CustomUserDetailsService;
import com.jeevan.TradingApp.service.EmailService;
import com.jeevan.TradingApp.service.TwoFactorOtpService;
import com.jeevan.TradingApp.utils.OtpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private TwoFactorOtpService twoFactorOtpService;
    @Autowired
    private EmailService emailService;
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
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(@RequestBody User user) throws Exception {
        String userName = user.getEmail();
        String password = user.getPassword();
        Authentication auth = authenticate(userName , password);
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = JwtProvider.generateToken(auth);
        User authUser = userRepository.findByEmail(userName);
        if(user.getTwoFactorAuth().isEnabled()){
            AuthResponse res = new AuthResponse();
            res.setMessage("two factor auth is enabled");
            res.setTwoFactorAuthEnabled(true);
            String otp = OtpUtils.generateOTP();
            TwoFactorOTP oldTwoFactorOTP = twoFactorOtpService.findByUser(authUser.getId());
            if(oldTwoFactorOTP != null){
                twoFactorOtpService.deleteTwoFactorOtp(oldTwoFactorOTP);
            }
            TwoFactorOTP newTwoFactorOtp = twoFactorOtpService.createTwoFactorOtp(authUser , otp , jwt);
            emailService.sendVerificationOtpEmail(userName , otp);
            res.setSession(newTwoFactorOtp.getId());
            return new ResponseEntity<>(res , HttpStatus.ACCEPTED);
        }
        AuthResponse res = new AuthResponse();
        res.setJwt(jwt);
        res.setStatus(true);
        res.setMessage("login  success");
        return new ResponseEntity<>(res , HttpStatus.CREATED);
    }

    private Authentication authenticate(String userName, String password) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
        if(userDetails == null){
            throw new BadCredentialsException("invalid user name");
        }
        if(!password.equals(userDetails.getPassword())){
            throw new BadCredentialsException("invalid  password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails , password ,userDetails.getAuthorities());
    }
    public ResponseEntity<AuthResponse> verifySigninOtp(@PathVariable String otp , @RequestParam String id) throws Exception {
        TwoFactorOTP twoFactorOTP = twoFactorOtpService.findById(id);
        if(twoFactorOtpService.verifyTwoFactorOtp(twoFactorOTP , otp)){
            AuthResponse res = new AuthResponse();
            res.setMessage("Two Factor Authentication verified");
            res.setTwoFactorAuthEnabled(true);
            res.setJwt(twoFactorOTP.getJwt());
            return new ResponseEntity<>(res , HttpStatus.OK);
        }
        throw new Exception("invalid otp");
    }
}
