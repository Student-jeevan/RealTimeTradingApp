package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.config.JwtProvider;
import com.jeevan.TradingApp.modal.TwoFactorOTP;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.UserRepository;
import com.jeevan.TradingApp.request.LoginRequest;
import com.jeevan.TradingApp.request.OtpVerifyRequest;
import com.jeevan.TradingApp.response.AuthResponse;
import com.jeevan.TradingApp.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private OtpService otpService;

    @Autowired
    private DeviceService deviceService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ========================
    // SIGNUP FLOW
    // ========================

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@RequestBody User user) {
        User emailExist = userRepository.findByEmail(user.getEmail());
        if (emailExist != null) {
            throw new com.jeevan.TradingApp.exception.CustomException("EMAIL_ALREADY_USED",
                    "Email already used by another account");
        }

        // Generate OTP via Redis
        String otp;
        try {
            otp = otpService.generateAndStoreOtp(user.getEmail());
        } catch (com.jeevan.TradingApp.exception.CustomException e) {
            throw e; // Re-throw cooldown errors
        }

        // Send OTP via email
        try {
            emailService.sendVerificationOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            otpService.clearOtp(user.getEmail());
            throw new com.jeevan.TradingApp.exception.CustomException("EMAIL_SEND_ERROR",
                    "Failed to send verification email");
        }

        // Store pending user in Redis (using a simple key for the signup session)
        // We still use in-memory for pending signups since it's short-lived
        pendingSignups.put(user.getEmail(), user);

        AuthResponse res = new AuthResponse();
        res.setStatus(true);
        res.setMessage("OTP sent to email");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    // In-memory storage for pending signups (data lost on restart — acceptable for short-lived signup flow)
    private final java.util.Map<String, User> pendingSignups = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/signup/verify")
    public ResponseEntity<AuthResponse> verifySignup(@RequestBody OtpVerifyRequest req) {
        String email = req.getEmail();
        String otp = req.getOtp();

        if (email == null || otp == null) {
            throw new com.jeevan.TradingApp.exception.CustomException("INVALID_INPUT",
                    "Email and OTP are required");
        }

        // Verify OTP via Redis
        otpService.verifyOtp(email, otp);

        User user = pendingSignups.get(email);
        if (user == null) {
            throw new com.jeevan.TradingApp.exception.CustomException("SESSION_EXPIRED",
                    "Signup session expired, please sign up again");
        }

        // Hash password with BCrypt before saving
        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setVerified(true);

        User savedUser = userRepository.save(newUser);
        watchlistService.createWatchlist(savedUser);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(),
                null,
                customUserDetailsService.loadUserByUsername(savedUser.getEmail()).getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = JwtProvider.generateToken(auth);

        // Cleanup
        pendingSignups.remove(email);

        AuthResponse res = new AuthResponse();
        res.setJwt(jwt);
        res.setStatus(true);
        res.setMessage("Registration successful");
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    // ========================
    // LOGIN FLOW (REFACTORED)
    // ========================

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();
        String deviceId = loginRequest.getDeviceId();

        // 1. Authenticate credentials
        Authentication auth = authenticate(email, password);

        // 2. Check if user is verified
        User authUser = userRepository.findByEmail(email);
        if (!authUser.isVerified()) {
            throw new com.jeevan.TradingApp.exception.CustomException("ACCOUNT_NOT_VERIFIED",
                    "Account not verified. Please complete signup verification first.");
        }

        // 3. Check if device is trusted
        if (deviceId != null && !deviceId.isBlank() && deviceService.isTrustedDevice(authUser, deviceId)) {
            // TRUSTED DEVICE → return JWT immediately, no OTP
            SecurityContextHolder.getContext().setAuthentication(auth);
            String jwt = JwtProvider.generateToken(auth);

            AuthResponse res = new AuthResponse();
            res.setJwt(jwt);
            res.setStatus(true);
            res.setMessage("Login successful");
            res.setTwoFactorAuthEnabled(false);
            res.setDeviceId(deviceId);
            return new ResponseEntity<>(res, HttpStatus.OK);
        }

        // 4. NEW DEVICE → require OTP
        String otp;
        try {
            otp = otpService.generateAndStoreOtp(email);
        } catch (com.jeevan.TradingApp.exception.CustomException e) {
            throw e;
        }

        try {
            emailService.sendVerificationOtpEmail(email, otp);
        } catch (Exception e) {
            otpService.clearOtp(email);
            throw new com.jeevan.TradingApp.exception.CustomException("EMAIL_SEND_ERROR",
                    "Failed to send verification email");
        }

        AuthResponse res = new AuthResponse();
        res.setMessage("OTP_REQUIRED");
        res.setTwoFactorAuthEnabled(true);
        res.setStatus(true);
        res.setSession(email); // frontend uses this to know which email to verify
        return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
    }

    // ========================
    // VERIFY OTP FOR LOGIN (NEW DEVICE)
    // ========================

    @PostMapping("/login/verify-otp")
    public ResponseEntity<AuthResponse> verifyLoginOtp(@RequestBody OtpVerifyRequest req) {
        String email = req.getEmail();
        String otp = req.getOtp();
        String deviceId = req.getDeviceId();

        if (email == null || otp == null) {
            throw new com.jeevan.TradingApp.exception.CustomException("INVALID_INPUT",
                    "Email and OTP are required");
        }

        // Verify OTP via Redis
        otpService.verifyOtp(email, otp);

        // Get user and generate JWT
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new com.jeevan.TradingApp.exception.CustomException("USER_NOT_FOUND",
                    "User not found");
        }

        // Mark device as trusted (30 days)
        if (deviceId != null && !deviceId.isBlank()) {
            deviceService.trustDevice(user, deviceId);
        }

        // Generate JWT
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = JwtProvider.generateToken(auth);

        AuthResponse res = new AuthResponse();
        res.setJwt(jwt);
        res.setStatus(true);
        res.setMessage("Login successful. Device trusted.");
        res.setTwoFactorAuthEnabled(false);
        res.setDeviceId(deviceId);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    // ========================
    // RESEND OTP
    // ========================

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            throw new com.jeevan.TradingApp.exception.CustomException("INVALID_INPUT",
                    "Email is required");
        }

        String otp = otpService.generateAndStoreOtp(email); // throws if cooldown active

        try {
            emailService.sendVerificationOtpEmail(email, otp);
        } catch (Exception e) {
            otpService.clearOtp(email);
            throw new com.jeevan.TradingApp.exception.CustomException("EMAIL_SEND_ERROR",
                    "Failed to send verification email");
        }

        AuthResponse res = new AuthResponse();
        res.setStatus(true);
        res.setMessage("OTP resent to email");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    // ========================
    // BACKWARD COMPATIBILITY: Old 2FA endpoint
    // ========================

    @PostMapping("/two-factor/otp/{otp}")
    public ResponseEntity<AuthResponse> verifySigninOtp(@PathVariable String otp, @RequestParam String id) {
        TwoFactorOTP twoFactorOTP = twoFactorOtpService.findById(id);
        if (twoFactorOTP != null && twoFactorOtpService.verifyTwoFactorOtp(twoFactorOTP, otp)) {
            AuthResponse res = new AuthResponse();
            res.setMessage("Two Factor Authentication verified");
            res.setTwoFactorAuthEnabled(true);
            res.setJwt(twoFactorOTP.getJwt());
            return new ResponseEntity<>(res, HttpStatus.OK);
        }
        throw new com.jeevan.TradingApp.exception.CustomException("INVALID_OTP", "Invalid OTP");
    }

    // ========================
    // AUTHENTICATION HELPER
    // ========================

    private Authentication authenticate(String userName, String password) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
        if (userDetails == null) {
            throw new BadCredentialsException("Invalid username");
        }

        // Support both BCrypt and legacy plaintext passwords
        boolean passwordMatch = passwordEncoder.matches(password, userDetails.getPassword())
                || password.equals(userDetails.getPassword());

        if (!passwordMatch) {
            throw new BadCredentialsException("Invalid password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }
}
