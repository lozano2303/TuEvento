package com.capysoft.tuevento.modules.storage.domain.repository;

import com.capysoft.tuevento.modules.storage.domain.model.StorageOperationLog;

import java.util.List;

public interface StorageOperationLogRepository {

    StorageOperationLog save(StorageOperationLog log);

    /** Returns all operation logs for a specific stored file. */
    List<StorageOperationLog> findByStoredFileId(Integer storedFileId);

    /** Returns all operations performed by a specific user. */
    List<StorageOperationLog> findByPerformedBy(Integer performedBy);

    /** Returns all logs for a given operation type (UPLOAD, DELETE, URL_GENERATED). */
    List<StorageOperationLog> findByOperation(String operation);
}
