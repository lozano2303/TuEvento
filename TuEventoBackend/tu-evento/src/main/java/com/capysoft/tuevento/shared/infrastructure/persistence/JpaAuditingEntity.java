package com.capysoft.tuevento.shared.infrastructure.persistence;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.capysoft.tuevento.shared.domain.model.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Implementación de infraestructura de {@link AuditableEntity}.
 * Clase abstracta base para todas las entidades JPA auditables del proyecto.
 *
 * Los campos de auditoría se auto-populan mediante Spring Data JPA:
 * - {@code createdAt} y {@code createdBy} solo se escriben en el insert.
 * - {@code updatedAt} y {@code updatedBy} se actualizan en cada cambio.
 *
 * Todas las entidades JPA del proyecto deben extender esta clase.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class JpaAuditingEntity implements AuditableEntity {

    /** Timestamp de creación. Inmutable tras el primer insert. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp de la última actualización. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Identificador del usuario que creó el registro. Inmutable tras el primer insert. */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /** Identificador del usuario que realizó la última modificación. */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
