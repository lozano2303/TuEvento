package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StorageOperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageOperationLogJpaRepository extends JpaRepository<StorageOperationLogEntity, Integer> {

    List<StorageOperationLogEntity> findByStoredFileStoredFileId(Integer storedFileId);

    List<StorageOperationLogEntity> findByPerformedBy(Integer performedBy);

    List<StorageOperationLogEntity> findByOperation(String operation);
}
