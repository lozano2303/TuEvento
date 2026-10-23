package com.capysoft.tuevento.modules.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Datos de un pago en el gateway externo.
 * Contrato genérico - no debe tener campos específicos de un proveedor.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayPayment {
    private String paymentId;         // ID del pago en el gateway
    private String externalReference; // Nuestra referencia (orderId)
    private String status;            // PENDING, APPROVED, DECLINED, etc.
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String createdAt;
}
