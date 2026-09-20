package com.capysoft.fakepaymentgateway.application.dto.request;

import com.capysoft.fakepaymentgateway.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request para crear un nuevo pago.
 */
public record CreatePaymentRequest(
    @NotBlank(message = "External reference is required")
    String externalReference,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    @NotBlank(message = "Currency is required")
    String currency,
    
    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod
) {
}
