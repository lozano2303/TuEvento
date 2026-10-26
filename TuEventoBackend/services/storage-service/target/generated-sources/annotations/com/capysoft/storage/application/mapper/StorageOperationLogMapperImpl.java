package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.StorageOperationLog;
import com.capysoft.storage.infrastructure.persistence.entity.StorageOperationLogEntity;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-25T22:48:41-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Ubuntu)"
)
@Component
public class StorageOperationLogMapperImpl implements StorageOperationLogMapper {

    @Autowired
    private StoredFileMapper storedFileMapper;

    @Override
    public StorageOperationLog toDomain(StorageOperationLogEntity entity) {
        if ( entity == null ) {
            return null;
        }

        StorageOperationLog.StorageOperationLogBuilder storageOperationLog = StorageOperationLog.builder();

        storageOperationLog.logId( entity.getLogId() );
        storageOperationLog.storedFile( storedFileMapper.toDomain( entity.getStoredFile() ) );
        storageOperationLog.performedBy( entity.getPerformedBy() );
        storageOperationLog.operation( entity.getOperation() );
        storageOperationLog.status( entity.getStatus() );
        storageOperationLog.errorMessage( entity.getErrorMessage() );
        storageOperationLog.occurredAt( entity.getOccurredAt() );

        return storageOperationLog.build();
    }

    @Override
    public StorageOperationLogEntity toEntity(StorageOperationLog domain) {
        if ( domain == null ) {
            return null;
        }

        StorageOperationLogEntity.StorageOperationLogEntityBuilder storageOperationLogEntity = StorageOperationLogEntity.builder();

        storageOperationLogEntity.logId( domain.getLogId() );
        storageOperationLogEntity.storedFile( storedFileMapper.toEntity( domain.getStoredFile() ) );
        storageOperationLogEntity.performedBy( domain.getPerformedBy() );
        storageOperationLogEntity.operation( domain.getOperation() );
        storageOperationLogEntity.status( domain.getStatus() );
        storageOperationLogEntity.errorMessage( domain.getErrorMessage() );
        storageOperationLogEntity.occurredAt( domain.getOccurredAt() );

        return storageOperationLogEntity.build();
    }
}
