package com.capysoft.tuevento.modules.storage.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.storage.application.mapper.StorageOperationLogMapper;
import com.capysoft.tuevento.modules.storage.domain.model.StorageOperationLog;
import com.capysoft.tuevento.modules.storage.domain.repository.StorageOperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StorageOperationLogRepositoryImpl implements StorageOperationLogRepository {

    private final StorageOperationLogJpaRepository jpaRepository;
    private final StorageOperationLogMapper mapper;

    @Override
    public StorageOperationLog save(StorageOperationLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }

    @Override
    public List<StorageOperationLog> findByStoredFileId(Integer storedFileId) {
        return jpaRepository.findByStoredFileStoredFileId(storedFileId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<StorageOperationLog> findByPerformedBy(Integer performedBy) {
        return jpaRepository.findByPerformedBy(performedBy).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<StorageOperationLog> findByOperation(String operation) {
        return jpaRepository.findByOperation(operation).stream().map(mapper::toDomain).toList();
    }
}
