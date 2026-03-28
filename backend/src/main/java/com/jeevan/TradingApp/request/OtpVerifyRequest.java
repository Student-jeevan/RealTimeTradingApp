package com.jeevan.TradingApp.request;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String email;
    private String otp;
    private String deviceId;
}
