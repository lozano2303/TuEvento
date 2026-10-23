package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StoredFileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OrganizerPetitionMapper {

    @org.mapstruct.Mapping(target = "storedFileId", source = "storedFile", qualifiedByName = "storedFileIdToInteger")
    OrganizerPetition toDomain(OrganizerPetitionEntity entity);

    @org.mapstruct.Mapping(target = "storedFile", source = "storedFileId", qualifiedByName = "storedFileIdToEntity")
    OrganizerPetitionEntity toEntity(OrganizerPetition domain);

    @org.mapstruct.Named("storedFileIdToEntity")
    default StoredFileEntity mapStoredFileIdToEntity(Integer storedFileId) {
        if (storedFileId == null) {
            return null;
        }

        StoredFileEntity storedFile = new StoredFileEntity();
        storedFile.setStoredFileId(storedFileId);
        return storedFile;
    }

    @org.mapstruct.Named("storedFileIdToInteger")
    default Integer mapStoredFileIdToInteger(StoredFileEntity storedFile) {
        return storedFile != null ? storedFile.getStoredFileId() : null;
    }
}
