package com.capysoft.tuevento.modules.wallet.domain.model;

/**
 * Tipo de movimiento de wallet.
 * CREDIT y REVERSAL suman al balance; PAYMENT resta; ADJUSTMENT puede sumar o restar.
 */
public enum WalletTransactionType {
    CREDIT,      // Crédito recibido (ej. cancelación de evento)
    PAYMENT,     // Descuento por compra (reserva o confirmación)
    REVERSAL,    // Reversión de un PAYMENT completado
    ADJUSTMENT   // Ajuste manual (solo ADMIN)
}
