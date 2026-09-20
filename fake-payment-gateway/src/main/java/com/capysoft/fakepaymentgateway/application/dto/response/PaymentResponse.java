package com.capysoft.fakepaymentgateway.application.dto.response;

import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.model.PaymentMethod;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response con información de un pago.
 */
public record PaymentResponse(
    String paymentId,
    String externalReference,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    PaymentMethod paymentMethod,
    Instant createdAt,
    Instant updatedAt
) {
    public static PaymentResponse fromDomain(Payment payment) {
        return new PaymentResponse(
            payment.getPaymentId(),
            payment.getExternalReference(),
            payment.getStatus(),
            payment.getMoney().getAmount(),
            payment.getMoney().getCurrency(),
            payment.getPaymentMethod(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
