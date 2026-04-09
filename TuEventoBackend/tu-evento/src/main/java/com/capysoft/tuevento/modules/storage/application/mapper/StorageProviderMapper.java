package com.capysoft.tuevento.modules.storage.application.mapper;

import com.capysoft.tuevento.modules.storage.domain.model.StorageProvider;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StorageProviderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StorageProviderMapper {

    StorageProvider toDomain(StorageProviderEntity entity);
    StorageProviderEntity toEntity(StorageProvider domain);
}
