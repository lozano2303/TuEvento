package com.capysoft.tuevento.modules.payment.domain.model;

/**
 * Gateways de pago soportados.
 */
public enum PaymentGateway {
    FAKE,    // Simulador para desarrollo local
    WOMPI    // Gateway real (sin implementar todavía)
}
