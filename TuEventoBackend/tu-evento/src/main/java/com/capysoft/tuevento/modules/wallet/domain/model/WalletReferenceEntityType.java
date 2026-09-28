package com.capysoft.tuevento.modules.wallet.domain.model;

/**
 * Tipo de entidad de negocio que originó un movimiento de wallet.
 */
public enum WalletReferenceEntityType {
    EVENT_CANCELLATION, // Crédito por cancelación de evento
    ORDER,              // Movimiento asociado a una orden de compra
    TICKET,             // Movimiento asociado a un ticket individual
    ADMIN_ADJUSTMENT    // Crédito o ajuste manual de administrador
}
