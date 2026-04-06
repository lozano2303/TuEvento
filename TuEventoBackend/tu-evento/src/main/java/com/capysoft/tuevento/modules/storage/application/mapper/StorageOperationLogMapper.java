package com.capysoft.tuevento.modules.storage.application.mapper;

import com.capysoft.tuevento.modules.storage.domain.model.StorageOperationLog;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StorageOperationLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StoredFileMapper.class})
public interface StorageOperationLogMapper {

    StorageOperationLog toDomain(StorageOperationLogEntity entity);
    StorageOperationLogEntity toEntity(StorageOperationLog domain);
}
