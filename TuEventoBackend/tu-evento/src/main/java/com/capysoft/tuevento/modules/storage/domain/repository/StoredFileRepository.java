package com.capysoft.tuevento.modules.storage.domain.repository;

import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;

import java.util.List;
import java.util.Optional;

public interface StoredFileRepository {

    StoredFile save(StoredFile storedFile);
    Optional<StoredFile> findById(Integer storedFileId);
    Optional<StoredFile> findByS3Key(String s3Key);

    /** Returns all non-deleted files uploaded by a specific user. */
    List<StoredFile> findByUploadedBy(Integer uploadedBy);

    /** Returns all non-deleted files belonging to a specific category. */
    List<StoredFile> findByFileCategoryId(Integer fileCategoryId);

    /** Returns all non-deleted files associated with a given owner entity. */
    List<StoredFile> findByOwnerEntity(Integer ownerEntityId, String ownerEntityType);

    /** Soft-deletes a file by marking it as deleted. */
    void softDelete(Integer storedFileId);

    /** Permanently removes a file record. */
    void deleteById(Integer storedFileId);
}
