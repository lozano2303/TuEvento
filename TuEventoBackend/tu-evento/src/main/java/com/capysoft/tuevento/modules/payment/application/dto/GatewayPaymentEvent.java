package com.capysoft.tuevento.modules.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Evento de pago recibido del gateway vía webhook.
 * Contrato genérico - no debe tener campos específicos de un proveedor.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayPaymentEvent {
    private String eventId;           // ID único del evento (para idempotencia)
    private String eventType;         // payment.approved, payment.declined, etc.
    private String paymentId;         // ID del pago en el gateway
    private String status;            // APPROVED, DECLINED, FAILED, etc.
    private BigDecimal amount;
    private String currency;
    private String timestamp;
}
