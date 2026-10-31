package com.capysoft.commerce.payment.domain.model;

/**
 * Estados de reembolso.
 * Estructura lista para fase futura (sin implementar flujo todavía).
 */
public enum RefundStatus {
    REQUESTED,
    APPROVED,
    PROCESSED,
    FAILED
}
