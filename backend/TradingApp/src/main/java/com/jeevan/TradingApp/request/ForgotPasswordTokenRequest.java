package com.jeevan.TradingApp.request;

import com.jeevan.TradingApp.domain.VerificationType;
import lombok.Data;

@Data
public class ForgotPasswordTokenRequest {
    private String sendTo;
    private VerificationType verificationType;
}
