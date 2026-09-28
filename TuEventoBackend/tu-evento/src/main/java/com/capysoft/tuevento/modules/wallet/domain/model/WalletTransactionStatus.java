package com.capysoft.tuevento.modules.wallet.domain.model;

/**
 * Estado de un movimiento de wallet.
 * Un WalletTransaction es inmutable en monto/tipo: solo cambia su status.
 */
public enum WalletTransactionStatus {
    PENDING,    // Reservado pero no confirmado (ej. PAYMENT esperando webhook)
    COMPLETED,  // Aplicado definitivamente al balance
    FAILED      // Cancelado / revertido sin efecto en balance
}
