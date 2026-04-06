package com.capysoft.tuevento.modules.storage.application.mapper;

import com.capysoft.tuevento.modules.storage.domain.model.FileCategory;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.FileCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StorageProviderMapper.class})
public interface FileCategoryMapper {

    FileCategory toDomain(FileCategoryEntity entity);
    FileCategoryEntity toEntity(FileCategory domain);
}
