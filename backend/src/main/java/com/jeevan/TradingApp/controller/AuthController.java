package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.config.JwtProvider;
import com.jeevan.TradingApp.modal.TwoFactorOTP;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import com.jeevan.TradingApp.response.AuthResponse;
import com.jeevan.TradingApp.service.CustomUserDetailsService;
import com.jeevan.TradingApp.service.EmailService;
import com.jeevan.TradingApp.service.TwoFactorOtpService;
import com.jeevan.TradingApp.service.WatchlistService;
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

    @Autowired
    private WatchlistService watchlistService;

    // In-memory storage for pending signups (Note: Uses concurrent maps for thread
    // safety, but data is lost on restart)
    // In a production environment, use Redis or a database table (e.g.,
    // VerificationCode entity)
    private java.util.Map<String, User> pendingSignups = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<String, String> signupOtps = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@RequestBody User user) throws Exception {
        User EmailExist = userRepository.findByEmail(user.getEmail());
        if (EmailExist != null) {
            throw new Exception("email already used by another account");
        }

        // Generate OTP
        String otp = OtpUtils.generateOTP();

        // Store temporary user data
        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(user.getPassword());
        // Note: Password should ideally be hashed here if not already, but the original
        // code did it in the "save" step or assumed raw?
        // Original code: newuser.setPassword(user.getPassword()); -> save.
        // We will preserve this behavior.

        pendingSignups.put(user.getEmail(), newUser);
        signupOtps.put(user.getEmail(), otp);

        // Send OTP
        emailService.sendVerificationOtpEmail(user.getEmail(), otp);

        AuthResponse res = new AuthResponse();
        res.setStatus(true);
        res.setMessage("OTP sent to email");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<AuthResponse> verifySignup(@RequestBody com.jeevan.TradingApp.modal.VerificationCode req)
            throws Exception {
        String email = req.getEmail();
        String otp = req.getOtp();

        if (email == null || otp == null) {
            throw new Exception("Email and OTP are required");
        }

        if (!signupOtps.containsKey(email) || !signupOtps.get(email).equals(otp)) {
            throw new Exception("Invalid OTP");
        }

        User user = pendingSignups.get(email);
        if (user == null) {
            throw new Exception("User session expired, please signup again");
        }

        // Persist User
        User saveduser = userRepository.save(user);
        watchlistService.createWatchlist(saveduser);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = JwtProvider.generateToken(auth);

        // Cleanup
        pendingSignups.remove(email);
        signupOtps.remove(email);

        AuthResponse res = new AuthResponse();
        res.setJwt(jwt);
        res.setStatus(true);
        res.setMessage("registered success");
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(@RequestBody User user) throws Exception {
        String userName = user.getEmail();
        String password = user.getPassword();
        Authentication auth = authenticate(userName, password);
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = JwtProvider.generateToken(auth);
        User authUser = userRepository.findByEmail(userName);

        // Enforce 2FA for all users
        AuthResponse res = new AuthResponse();
        res.setMessage("two factor auth is enabled");
        res.setTwoFactorAuthEnabled(true);
        String otp = OtpUtils.generateOTP();
        TwoFactorOTP oldTwoFactorOTP = twoFactorOtpService.findByUser(authUser.getId());
        if (oldTwoFactorOTP != null) {
            twoFactorOtpService.deleteTwoFactorOtp(oldTwoFactorOTP);
        }
        TwoFactorOTP newTwoFactorOtp = twoFactorOtpService.createTwoFactorOtp(authUser, otp, jwt);
        emailService.sendVerificationOtpEmail(userName, otp);
        res.setSession(newTwoFactorOtp.getId());
        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }

    private Authentication authenticate(String userName, String password) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
        if (userDetails == null) {
            throw new BadCredentialsException("invalid user name");
        }
        if (!password.equals(userDetails.getPassword())) {
            throw new BadCredentialsException("invalid  password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    @PostMapping("/two-factor/otp/{otp}")
    public ResponseEntity<AuthResponse> verifySigninOtp(@PathVariable String otp, @RequestParam String id)
            throws Exception {
        TwoFactorOTP twoFactorOTP = twoFactorOtpService.findById(id);
        if (twoFactorOtpService.verifyTwoFactorOtp(twoFactorOTP, otp)) {
            AuthResponse res = new AuthResponse();
            res.setMessage("Two Factor Authentication verified");
            res.setTwoFactorAuthEnabled(true);
            res.setJwt(twoFactorOTP.getJwt());
            return new ResponseEntity<>(res, HttpStatus.OK);
        }
        throw new Exception("invalid otp");
    }
}
