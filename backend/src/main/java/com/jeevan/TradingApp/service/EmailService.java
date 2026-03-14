package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.domain.AlertCondition;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    public void sendPriceAlertEmail(String email, String coin, BigDecimal targetPrice,
            BigDecimal currentPrice, AlertCondition condition) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String direction = condition == AlertCondition.ABOVE ? "risen above" : "fallen below";
        String arrow = condition == AlertCondition.ABOVE ? "🚀" : "📉";
        String color = condition == AlertCondition.ABOVE ? "#16a34a" : "#dc2626";
        String subject = arrow + " Price Alert Triggered: " + coin.toUpperCase();

        String text = """
                <div style="font-family: Helvetica, Arial, sans-serif; background-color: #f9f9f9; padding: 40px 20px; color: #333;">
                  <div style="max-width: 520px; margin: 0 auto; background: #fff; padding: 32px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.06);">
                    <h1 style="color: #1a1a1a; font-size: 22px; margin: 0 0 8px;">Crypto Trading — Price Alert %s</h1>
                    <p style="color: #555; margin-bottom: 24px; font-size: 15px;">
                      Your alert for <strong>%s</strong> has been triggered.
                    </p>
                    <table style="width:100%%;border-collapse:collapse;font-size:15px;">
                      <tr style="background:#f4f4f5;">
                        <td style="padding:10px 14px;">Coin</td>
                        <td style="padding:10px 14px;font-weight:700;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px 14px;">Condition</td>
                        <td style="padding:10px 14px;">Price has <strong>%s</strong> your target</td>
                      </tr>
                      <tr style="background:#f4f4f5;">
                        <td style="padding:10px 14px;">Target Price</td>
                        <td style="padding:10px 14px;">$%s</td>
                      </tr>
                      <tr>
                        <td style="padding:10px 14px;">Current Price</td>
                        <td style="padding:10px 14px; color:%s; font-weight:700;">$%s</td>
                      </tr>
                    </table>
                    <p style="margin-top:28px;font-size:12px;color:#999;">This alert will not fire again. Log in to set a new alert.</p>
                  </div>
                </div>
                """
                .formatted(arrow, coin.toUpperCase(), coin.toUpperCase(), direction,
                        targetPrice.toPlainString(), color, currentPrice.toPlainString());

        helper.setSubject(subject);
        helper.setText(text, true);
        helper.setTo(email);
        try {
            javaMailSender.send(mimeMessage);
        } catch (MailException e) {
            throw new MailSendException(e.getMessage());
        }
    }
}
