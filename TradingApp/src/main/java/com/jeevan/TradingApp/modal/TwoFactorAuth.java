package com.jeevan.TradingApp.modal;
import com.jeevan.TradingApp.domain.VerificationType;
import lombok.Data;
@Data
public class TwoFactorAuth {
    private boolean isEnabled = false;
    private VerificationType sentTo;
}
