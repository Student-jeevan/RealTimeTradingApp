package com.jeevan.TradingApp.controller;

import com.jeevan.TradingApp.request.ForgotPasswordTokenRequest;
import com.jeevan.TradingApp.domain.VerificationType;
import com.jeevan.TradingApp.modal.ForgotPasswordToken;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.modal.VerificationCode;
import com.jeevan.TradingApp.request.ResetPasswordRequest;
import com.jeevan.TradingApp.response.ApiResponse;
import com.jeevan.TradingApp.response.AuthResponse;
import com.jeevan.TradingApp.service.*;
import com.jeevan.TradingApp.utils.OtpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<User> getUserProfile(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        return new ResponseEntity<>(user , HttpStatus.OK);
    }

    @PostMapping("/api/users/verification/{verificationType}/send-otp")
    public ResponseEntity<String> sendVerificationOtp(@RequestHeader("Authorization") String jwt , @PathVariable VerificationType verificationType) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        VerificationCode verificationCode = verificationCodeService.getVerificationCodeByUser(user.getId());

        if(verificationCode == null){
            verificationCode=verificationCodeService.sendVerificationCode(user , verificationType);
        }
        if(verificationType.equals(VerificationType.EMAIL)){
            emailService.sendVerificationOtpEmail(user.getEmail() , verificationCode.getOtp());
        }

        return new ResponseEntity<>("verification otp sent successfully" , HttpStatus.OK);
    }

    @PatchMapping("/api/users/enable-two-factor/verify-otp/{otp}")
    public ResponseEntity<User> enableTwoFactorAuthentication(@PathVariable String otp  ,   @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        VerificationCode verificationCode = verificationCodeService.getVerificationCodeByUser(user.getId());

        String sendTo = verificationCode.getVerificationType().equals(VerificationType.EMAIL)?verificationCode.getEmail():verificationCode.getMobile();
        boolean isVerified = verificationCode.getOtp().equals(otp);
        if(isVerified){
            User updatedUser = userService.enableTwoFactorAuthentication(verificationCode.getVerificationType() , sendTo , user);
            verificationCodeService.deleteVerificationCodeById(verificationCode);
            return new ResponseEntity<>(updatedUser , HttpStatus.OK);
        }
        throw new Exception("incorrect otp");
    }

    @PostMapping("/auth/users/reset-password/send-otp")
    public ResponseEntity<AuthResponse> sendForgotPasswordOtp(@RequestBody ForgotPasswordTokenRequest request) throws Exception {
        logger.info("Received forgot password request for: {}", request.getSendTo());
        logger.info("Verification type: {}", request.getVerificationType());
        
        try {
            User user = userService.findUserByEmail(request.getSendTo());
            logger.info("User found: {}", user.getEmail());
            
            String otp = OtpUtils.generateOTP();
            ForgotPasswordToken token = forgotPasswordService.findByUser(user.getId());
            if(token == null){
                UUID uuid = UUID.randomUUID();
                String id = uuid.toString();
                token = forgotPasswordService.createToken(user , id , otp ,request.getVerificationType() , request.getSendTo());
                logger.info("Created new forgot password token with ID: {}", id);
            } else {
                // Update existing token with new OTP
                token = forgotPasswordService.updateToken(token, otp, request.getVerificationType(), request.getSendTo());
                logger.info("Updated existing forgot password token with ID: {} with new OTP", token.getId());
            }
            if(request.getVerificationType().equals(VerificationType.EMAIL)){
                emailService.sendVerificationOtpEmail(user.getEmail() , token.getOtp());
                logger.info("Verification OTP email sent to: {}", user.getEmail());
            }
            AuthResponse response = new AuthResponse();
            response.setSession(token.getId());
            response.setMessage("Password reset otp sent successfully");
            logger.info("Successfully processed forgot password request");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error processing forgot password request: ", e);
            throw e;
        }
    }
    @PatchMapping("/auth/users/reset-password/verify-otp")
    public ResponseEntity<ApiResponse> resetPassword(
            @RequestParam String id,
            @RequestBody ResetPasswordRequest req) throws Exception {
            logger.info("Received password reset request with token ID: {}", id);
            ForgotPasswordToken forgotPasswordToken  = forgotPasswordService.findById(id);
            if(forgotPasswordToken == null){
                logger.error("Forgot password token not found for ID: {}", id);
                throw new Exception("Invalid or expired reset token");
            }
            boolean isVerified = forgotPasswordToken.getOtp().equals(req.getOtp());
            if(isVerified){
                logger.info("OTP verified successfully for user: {}", forgotPasswordToken.getUser().getEmail());
                userService.updatePassword(forgotPasswordToken.getUser() , req.getPassword());
                // Delete the token after successful password reset
                forgotPasswordService.deleteToken(forgotPasswordToken);
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setMessage("password updated successfully");
                logger.info("Password updated successfully for user: {}", forgotPasswordToken.getUser().getEmail());
                return new ResponseEntity<>(apiResponse , HttpStatus.ACCEPTED);
            }
            logger.warn("Invalid OTP provided for token ID: {}", id);
            throw new Exception("wrong otp");
    }
}
