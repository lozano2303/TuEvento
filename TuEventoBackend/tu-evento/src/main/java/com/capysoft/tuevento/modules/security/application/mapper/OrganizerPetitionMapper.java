package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import com.capysoft.tuevento.modules.storage.infrastructure.persistence.entity.StoredFileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OrganizerPetitionMapper {

    @Mapping(target = "storedFileId", source = "storedFile.storedFileId")
    OrganizerPetition toDomain(OrganizerPetitionEntity entity);

    @Mapping(target = "storedFile",
             expression = "java(domain.getStoredFileId() != null ? buildStoredFileRef(domain.getStoredFileId()) : null)")
    OrganizerPetitionEntity toEntity(OrganizerPetition domain);

    default StoredFileEntity buildStoredFileRef(Integer storedFileId) {
        StoredFileEntity ref = new StoredFileEntity();
        ref.setStoredFileId(storedFileId);
        return ref;
    }
}
