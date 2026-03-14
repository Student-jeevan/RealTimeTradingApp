package com.jeevan.TradingApp.exception;

import java.time.Instant;

public class CustomException extends RuntimeException {

    private final String errorCode;
    private final String message;
    private final Instant timestamp;

    public CustomException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR"; // Default error code for this constructor
        this.message = message;
        this.timestamp = Instant.now(); // Corrected to Instant.now() for consistency
    }

    public CustomException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public CustomException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
