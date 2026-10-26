package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.FileCategory;
import com.capysoft.storage.infrastructure.persistence.entity.FileCategoryEntity;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-25T22:48:41-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Ubuntu)"
)
@Component
public class FileCategoryMapperImpl implements FileCategoryMapper {

    @Autowired
    private StorageProviderMapper storageProviderMapper;

    @Override
    public FileCategory toDomain(FileCategoryEntity entity) {
        if ( entity == null ) {
            return null;
        }

        FileCategory.FileCategoryBuilder fileCategory = FileCategory.builder();

        fileCategory.fileCategoryId( entity.getFileCategoryId() );
        fileCategory.storageProvider( storageProviderMapper.toDomain( entity.getStorageProvider() ) );
        fileCategory.name( entity.getName() );
        fileCategory.code( entity.getCode() );
        fileCategory.isPublic( entity.getIsPublic() );
        fileCategory.allowedExtensions( entity.getAllowedExtensions() );
        fileCategory.maxSizeMb( entity.getMaxSizeMb() );
        fileCategory.createdAt( entity.getCreatedAt() );
        fileCategory.updatedAt( entity.getUpdatedAt() );
        fileCategory.createdBy( entity.getCreatedBy() );
        fileCategory.updatedBy( entity.getUpdatedBy() );

        return fileCategory.build();
    }

    @Override
    public FileCategoryEntity toEntity(FileCategory domain) {
        if ( domain == null ) {
            return null;
        }

        FileCategoryEntity.FileCategoryEntityBuilder fileCategoryEntity = FileCategoryEntity.builder();

        fileCategoryEntity.fileCategoryId( domain.getFileCategoryId() );
        fileCategoryEntity.storageProvider( storageProviderMapper.toEntity( domain.getStorageProvider() ) );
        fileCategoryEntity.name( domain.getName() );
        fileCategoryEntity.code( domain.getCode() );
        fileCategoryEntity.isPublic( domain.getIsPublic() );
        fileCategoryEntity.allowedExtensions( domain.getAllowedExtensions() );
        fileCategoryEntity.maxSizeMb( domain.getMaxSizeMb() );

        return fileCategoryEntity.build();
    }
}
