package com.jeevan.TradingApp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;

    public void sendVerificationOtpEmail(String email, String otp) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, "utf-8");
        String subject = "Verify OTP - Crypto Trading";
        String text = """
                <div style="font-family: Helvetica, Arial, sans-serif; background-color: #f9f9f9; padding: 40px 20px; color: #333;">
                    <div style="max-width: 500px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <h1 style="color: #1a1a1a; font-size: 24px; font-weight: 700; margin: 0;">Crypto Trading</h1>
                        </div>
                        <div style="text-align: center;">
                            <p style="font-size: 16px; color: #555; margin-bottom: 20px;">Use the following OTP to verify your account:</p>
                            <div style="background-color: #f4f4f5; padding: 15px; border-radius: 8px; display: inline-block; margin-bottom: 20px;">
                                <span style="font-size: 32px; font-weight: 700; letter-spacing: 5px; color: #000;">%s</span>
                            </div>
                            <p style="font-size: 14px; color: #777; margin-bottom: 5px;">This OTP is valid for 5 minutes.</p>
                            <p style="font-size: 12px; color: #999; margin-top: 30px;">If you did not request this, please ignore this email.</p>
                        </div>
                    </div>
                </div>
                """
                .formatted(otp);
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(text, true);
        mimeMessageHelper.setTo(email);
        try {
            javaMailSender.send(mimeMessage);
        } catch (MailException e) {
            throw new MailSendException(e.getMessage());
        }
    }
}
