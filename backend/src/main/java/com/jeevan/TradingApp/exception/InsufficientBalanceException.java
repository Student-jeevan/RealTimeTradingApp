package com.jeevan.TradingApp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientBalanceException extends CustomException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
