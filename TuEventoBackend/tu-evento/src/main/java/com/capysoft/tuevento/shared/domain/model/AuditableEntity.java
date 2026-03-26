package com.capysoft.tuevento.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Abstract base class for all auditable JPA entities.
 * Automatically populates createdAt on insert and updatedAt on every update
 * via Spring Data JPA auditing ({@link AuditingEntityListener}).
 *
 * All domain entities that require audit tracking must extend this class.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    /** Timestamp when the record was first persisted. Never updated after creation. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp of the most recent update to the record. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** ID of the user who created the record. Nullable for system-generated records. */
    @CreatedBy
    @Column(name = "created_by")
    private Integer createdBy;

    /** ID of the user who last modified the record. Nullable for system-generated records. */
    @LastModifiedBy
    @Column(name = "updated_by")
    private Integer updatedBy;
}
