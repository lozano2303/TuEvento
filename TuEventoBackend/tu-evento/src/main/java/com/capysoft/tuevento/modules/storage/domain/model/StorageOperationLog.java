package com.capysoft.tuevento.modules.storage.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable log of a storage operation.
 * Not auditable — records are append-only and never modified.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageOperationLog {

    private Integer logId;
    private StoredFile storedFile;

    /** ID of the user who performed the operation. */
    private Integer performedBy;

    /** Operation type: UPLOAD, DELETE, URL_GENERATED. */
    private String operation;

    /** Result status: SUCCESS, FAILED. */
    private String status;

    private String errorMessage;
    private LocalDateTime occurredAt;
}
