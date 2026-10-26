package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.StorageOperationLog;
import com.capysoft.storage.infrastructure.persistence.entity.StorageOperationLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StoredFileMapper.class})
public interface StorageOperationLogMapper {

    StorageOperationLog toDomain(StorageOperationLogEntity entity);
    StorageOperationLogEntity toEntity(StorageOperationLog domain);
}
