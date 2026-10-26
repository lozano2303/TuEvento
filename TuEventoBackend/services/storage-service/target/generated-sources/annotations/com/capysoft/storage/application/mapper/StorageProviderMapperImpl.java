package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.StorageProvider;
import com.capysoft.storage.infrastructure.persistence.entity.StorageProviderEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-25T22:48:41-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Ubuntu)"
)
@Component
public class StorageProviderMapperImpl implements StorageProviderMapper {

    @Override
    public StorageProvider toDomain(StorageProviderEntity entity) {
        if ( entity == null ) {
            return null;
        }

        StorageProvider.StorageProviderBuilder storageProvider = StorageProvider.builder();

        storageProvider.storageProviderId( entity.getStorageProviderId() );
        storageProvider.name( entity.getName() );
        storageProvider.code( entity.getCode() );
        storageProvider.isActive( entity.getIsActive() );
        storageProvider.config( entity.getConfig() );
        storageProvider.createdAt( entity.getCreatedAt() );
        storageProvider.updatedAt( entity.getUpdatedAt() );
        storageProvider.createdBy( entity.getCreatedBy() );
        storageProvider.updatedBy( entity.getUpdatedBy() );

        return storageProvider.build();
    }

    @Override
    public StorageProviderEntity toEntity(StorageProvider domain) {
        if ( domain == null ) {
            return null;
        }

        StorageProviderEntity.StorageProviderEntityBuilder storageProviderEntity = StorageProviderEntity.builder();

        storageProviderEntity.storageProviderId( domain.getStorageProviderId() );
        storageProviderEntity.name( domain.getName() );
        storageProviderEntity.code( domain.getCode() );
        storageProviderEntity.isActive( domain.getIsActive() );
        storageProviderEntity.config( domain.getConfig() );

        return storageProviderEntity.build();
    }
}
