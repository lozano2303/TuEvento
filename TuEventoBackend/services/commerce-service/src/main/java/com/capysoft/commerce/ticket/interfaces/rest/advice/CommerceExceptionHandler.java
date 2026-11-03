package com.capysoft.commerce.ticket.interfaces.rest.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.capysoft.commerce.payment.domain.model.InvalidPaymentStatusTransitionException;
import com.capysoft.commerce.ticket.domain.model.InvalidOrderStatusTransitionException;
import com.capysoft.commerce.wallet.domain.model.InsufficientWalletBalanceException;
import com.capysoft.common.interfaces.ApiResponse;

/**
 * Errores propios de Comercio (ticket, payment y wallet). Vive con Comercio para que
 * {@code shared/} no conozca sus módulos y se vaya con ellos a commerce-service.
 *
 * <p>Va antes que {@code GlobalExceptionHandler}: ese advice tiene un handler para
 * {@code Exception}, y si se consultara primero estas excepciones acabarían en un 500.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommerceExceptionHandler {

    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPaymentStatusTransition(InvalidPaymentStatusTransitionException ex) {
        log.warn("Invalid payment status transition — {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrderStatusTransition(InvalidOrderStatusTransitionException ex) {
        log.warn("Invalid order status transition — {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientWalletBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientWalletBalance(InsufficientWalletBalanceException ex) {
        log.warn("Insufficient wallet balance — {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }
}
