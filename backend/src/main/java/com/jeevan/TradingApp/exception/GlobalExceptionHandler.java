package com.jeevan.TradingApp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ApiErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderValidation(OrderValidationException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedAccessException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(RiskValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleRiskValidation(RiskValidationException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ApiErrorResponse body = buildErrorResponse(status, "Invalid credentials", request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ApiErrorResponse body = buildErrorResponse(status, "User not found: " + ex.getMessage(),
                request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiErrorResponse> handleCustomException(CustomException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = (fieldError != null)
                ? fieldError.getField() + " " + fieldError.getDefaultMessage()
                : "Validation failed for request";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiErrorResponse body = buildErrorResponse(status, message, request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse body = buildErrorResponse(status, ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(body, status);
    }
}
