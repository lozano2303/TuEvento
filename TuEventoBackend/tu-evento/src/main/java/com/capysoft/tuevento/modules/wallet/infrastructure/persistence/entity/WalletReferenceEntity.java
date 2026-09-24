package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para WalletReference.
 * Inmutable — no tiene columnas de auditoría ni versión.
 */
@Entity
@Table(name = "wallet_reference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_reference_id")
    private Long walletReferenceId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private WalletReferenceEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;
}
