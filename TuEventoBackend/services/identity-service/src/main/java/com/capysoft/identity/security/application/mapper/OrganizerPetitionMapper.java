package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.OrganizerPetition;
import com.capysoft.identity.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OrganizerPetitionMapper {

    OrganizerPetition toDomain(OrganizerPetitionEntity entity);

    OrganizerPetitionEntity toEntity(OrganizerPetition domain);
}
