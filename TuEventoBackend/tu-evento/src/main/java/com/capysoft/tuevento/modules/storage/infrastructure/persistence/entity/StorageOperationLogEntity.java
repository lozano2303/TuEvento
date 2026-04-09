package com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable log entity — append-only, never updated.
 * Does not extend JpaAuditingEntity intentionally.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_operation_log")
public class StorageOperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id", nullable = false)
    private StoredFileEntity storedFile;

    /** ID of the user who performed the operation — no JPA relation to avoid cross-module coupling. */
    @Column(name = "performed_by", nullable = false)
    private Integer performedBy;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
