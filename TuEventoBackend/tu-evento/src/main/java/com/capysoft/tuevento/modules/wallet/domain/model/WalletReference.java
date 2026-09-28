package com.capysoft.tuevento.modules.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Trazabilidad del "por qué" de cada movimiento de wallet.
 * Inmutable — nunca se modifica tras su creación.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletReference {
    private Long walletReferenceId;
    private Long transactionId;
    private WalletReferenceEntityType entityType;
    private Long entityId;
}
