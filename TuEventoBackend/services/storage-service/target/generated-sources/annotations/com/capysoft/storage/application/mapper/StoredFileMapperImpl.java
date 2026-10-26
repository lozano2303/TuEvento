package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.infrastructure.persistence.entity.StoredFileEntity;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-25T22:48:41-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Ubuntu)"
)
@Component
public class StoredFileMapperImpl implements StoredFileMapper {

    @Autowired
    private FileCategoryMapper fileCategoryMapper;

    @Override
    public StoredFile toDomain(StoredFileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        StoredFile.StoredFileBuilder storedFile = StoredFile.builder();

        storedFile.storedFileId( entity.getStoredFileId() );
        storedFile.fileCategory( fileCategoryMapper.toDomain( entity.getFileCategory() ) );
        storedFile.uploadedBy( entity.getUploadedBy() );
        storedFile.s3Key( entity.getS3Key() );
        storedFile.originalFilename( entity.getOriginalFilename() );
        storedFile.contentType( entity.getContentType() );
        storedFile.sizeBytes( entity.getSizeBytes() );
        storedFile.publicUrl( entity.getPublicUrl() );
        storedFile.ownerEntityId( entity.getOwnerEntityId() );
        storedFile.ownerEntityType( entity.getOwnerEntityType() );
        storedFile.deleted( entity.getDeleted() );
        storedFile.uploadedAt( entity.getUploadedAt() );
        storedFile.createdAt( entity.getCreatedAt() );
        storedFile.updatedAt( entity.getUpdatedAt() );
        storedFile.createdBy( entity.getCreatedBy() );
        storedFile.updatedBy( entity.getUpdatedBy() );

        return storedFile.build();
    }

    @Override
    public StoredFileEntity toEntity(StoredFile domain) {
        if ( domain == null ) {
            return null;
        }

        StoredFileEntity.StoredFileEntityBuilder storedFileEntity = StoredFileEntity.builder();

        storedFileEntity.storedFileId( domain.getStoredFileId() );
        storedFileEntity.fileCategory( fileCategoryMapper.toEntity( domain.getFileCategory() ) );
        storedFileEntity.uploadedBy( domain.getUploadedBy() );
        storedFileEntity.s3Key( domain.getS3Key() );
        storedFileEntity.originalFilename( domain.getOriginalFilename() );
        storedFileEntity.contentType( domain.getContentType() );
        storedFileEntity.sizeBytes( domain.getSizeBytes() );
        storedFileEntity.publicUrl( domain.getPublicUrl() );
        storedFileEntity.ownerEntityId( domain.getOwnerEntityId() );
        storedFileEntity.ownerEntityType( domain.getOwnerEntityType() );
        storedFileEntity.deleted( domain.getDeleted() );
        storedFileEntity.uploadedAt( domain.getUploadedAt() );

        return storedFileEntity.build();
    }
}
