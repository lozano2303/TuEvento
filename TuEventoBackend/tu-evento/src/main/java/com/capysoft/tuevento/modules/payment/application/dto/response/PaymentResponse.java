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
    private BigDecimal amount;               // Monto cobrado por pasarela
    private String currency;
    private String paymentMethod;
    private BigDecimal walletAmountApplied;  // Monto cubierto por wallet (0 si no aplica)
    private BigDecimal amountToPayViaGateway; // Alias legible de amount (para el frontend)
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    public static PaymentResponse fromDomain(Payment payment) {
        BigDecimal gatewayAmount = payment.getAmount() != null
            ? payment.getAmount().getAmount() : BigDecimal.ZERO;
        BigDecimal walletApplied = payment.getWalletAmountApplied() != null
            ? payment.getWalletAmountApplied() : BigDecimal.ZERO;

        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .orderId(payment.getOrderId())
            .gateway(payment.getGateway())
            .gatewayTransactionId(payment.getGatewayTransactionId())
            .status(payment.getStatus())
            .amount(gatewayAmount)
            .currency(payment.getAmount() != null ? payment.getAmount().getCurrency() : "COP")
            .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
            .walletAmountApplied(walletApplied)
            .amountToPayViaGateway(gatewayAmount)
            .processedAt(payment.getProcessedAt())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
