package com.capysoft.storage.application.mapper;

import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.infrastructure.persistence.entity.StoredFileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FileCategoryMapper.class})
public interface StoredFileMapper {

    StoredFile toDomain(StoredFileEntity entity);
    StoredFileEntity toEntity(StoredFile domain);
}
