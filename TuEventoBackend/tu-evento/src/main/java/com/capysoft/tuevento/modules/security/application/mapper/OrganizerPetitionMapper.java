package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StoredFileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OrganizerPetitionMapper {

    @org.mapstruct.Mapping(target = "storedFileId", source = "storedFile", qualifiedByName = "storedFileIdToInteger")
    OrganizerPetition toDomain(OrganizerPetitionEntity entity);

    @org.mapstruct.Mapping(target = "storedFile", ignore = true)
    OrganizerPetitionEntity toEntity(OrganizerPetition domain);

    @org.mapstruct.Named("storedFileIdToInteger")
    default Integer mapStoredFileIdToInteger(StoredFileEntity storedFile) {
        return storedFile != null ? storedFile.getStoredFileId() : null;
    }
}
