package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.StorageProvider;
import com.capysoft.storage.infrastructure.persistence.entity.StorageProviderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StorageProviderMapper {

    StorageProvider toDomain(StorageProviderEntity entity);
    StorageProviderEntity toEntity(StorageProvider domain);
}
