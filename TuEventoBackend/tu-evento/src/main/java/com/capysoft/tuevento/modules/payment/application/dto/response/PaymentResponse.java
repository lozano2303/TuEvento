package com.capysoft.tuevento.modules.payment.application.dto.response;

import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentGateway;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response de pago.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private PaymentGateway gateway;
    private String gatewayTransactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    
    public static PaymentResponse fromDomain(Payment payment) {
        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .orderId(payment.getOrderId())
            .gateway(payment.getGateway())
            .gatewayTransactionId(payment.getGatewayTransactionId())
            .status(payment.getStatus())
            .amount(payment.getAmount().getAmount())
            .currency(payment.getAmount().getCurrency())
            .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
            .processedAt(payment.getProcessedAt())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
