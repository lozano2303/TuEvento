package com.capysoft.tuevento.shared.domain.model;

import java.time.LocalDateTime;

/**
 * Contrato de dominio para entidades auditables.
 * Define los campos de auditoría estándar sin ninguna dependencia
 * de JPA, Hibernate ni ningún framework de infraestructura.
 *
 * La implementación de persistencia se delega a {@code JpaAuditingEntity}.
 */
public interface AuditableEntity {

    /** Fecha y hora en que el registro fue creado. */
    LocalDateTime getCreatedAt();

    /** Fecha y hora de la última modificación del registro. */
    LocalDateTime getUpdatedAt();

    /** Identificador del usuario que creó el registro. Puede ser nulo para registros del sistema. */
    String getCreatedBy();

    /** Identificador del usuario que realizó la última modificación. Puede ser nulo para registros del sistema. */
    String getUpdatedBy();
}
