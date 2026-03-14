package com.jeevan.TradingApp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAccessException extends CustomException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
