package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OrganizerPetitionMapper {

    @Mapping(target = "storedFileId", source = "storedFile.storedFileId")
    OrganizerPetition toDomain(OrganizerPetitionEntity entity);

    @Mapping(target = "storedFile", ignore = true)
    OrganizerPetitionEntity toEntity(OrganizerPetition domain);
}
