package com.capysoft.tuevento.modules.storage.application.mapper;

import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StoredFileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FileCategoryMapper.class})
public interface StoredFileMapper {

    StoredFile toDomain(StoredFileEntity entity);
    StoredFileEntity toEntity(StoredFile domain);
}
