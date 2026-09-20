package com.capysoft.fakepaymentgateway.interfaces.rest;

import com.capysoft.fakepaymentgateway.application.usecase.GetPaymentUseCase;
import com.capysoft.fakepaymentgateway.domain.model.InvalidStatusTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Manejador global de excepciones para los controladores REST.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(GetPaymentUseCase.PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFound(
        GetPaymentUseCase.PaymentNotFoundException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "Payment not found",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
            ));
    }
    
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(
        InvalidStatusTransitionException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "Invalid status transition",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
            ));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
        IllegalArgumentException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "Invalid argument",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
            ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Internal server error",
                "message", ex.getMessage(),
                "timestamp", Instant.now()
            ));
    }
}
