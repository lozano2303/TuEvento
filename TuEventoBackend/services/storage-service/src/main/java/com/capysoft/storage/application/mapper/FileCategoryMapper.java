package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.FileCategory;
import com.capysoft.storage.infrastructure.persistence.entity.FileCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StorageProviderMapper.class})
public interface FileCategoryMapper {

    FileCategory toDomain(FileCategoryEntity entity);
    FileCategoryEntity toEntity(FileCategory domain);
}
