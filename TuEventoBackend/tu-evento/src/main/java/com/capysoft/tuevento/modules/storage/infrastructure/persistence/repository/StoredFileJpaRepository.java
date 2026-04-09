package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StoredFileEntity;

public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, Integer> {

    Optional<StoredFileEntity> findByS3Key(String s3Key);

    @Query("SELECT f FROM StoredFileEntity f WHERE f.uploadedBy = :uploadedBy AND f.deleted = false")
    List<StoredFileEntity> findByUploadedBy(@Param("uploadedBy") Integer uploadedBy);

    @Query("SELECT f FROM StoredFileEntity f WHERE f.fileCategory.fileCategoryId = :fileCategoryId AND f.deleted = false")
    List<StoredFileEntity> findByFileCategoryId(@Param("fileCategoryId") Integer fileCategoryId);

    @Query("SELECT sf FROM StoredFileEntity sf JOIN FETCH sf.fileCategory fc JOIN FETCH fc.storageProvider WHERE sf.ownerEntityId = :ownerEntityId AND sf.ownerEntityType = :ownerEntityType AND sf.deleted = false")
    List<StoredFileEntity> findByOwnerEntity(@Param("ownerEntityId") Integer ownerEntityId,
                                             @Param("ownerEntityType") String ownerEntityType);

    @Modifying
    @Query("UPDATE StoredFileEntity f SET f.deleted = true WHERE f.storedFileId = :storedFileId")
    void softDelete(@Param("storedFileId") Integer storedFileId);
}
