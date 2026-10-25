package com.capysoft.fakepaymentgateway.application.dto.request;

import com.capysoft.fakepaymentgateway.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request para crear un nuevo pago.
 * Los límites coinciden con las columnas de la tabla payments.
 */
public record CreatePaymentRequest(
    @NotBlank(message = "External reference is required")
    @Size(max = 100, message = "External reference must be at most 100 characters")
    String externalReference,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "[A-Za-z]{3}", message = "Currency must be a 3-letter ISO 4217 code")
    String currency,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod
) {
}
