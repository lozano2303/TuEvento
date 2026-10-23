package com.capysoft.tuevento.modules.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Comando para crear un pago en el gateway externo.
 * Contrato genérico - no debe tener campos específicos de un proveedor.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentCommand {
    private String externalReference; // Nuestra referencia (orderId)
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
}
