package com.capysoft.identity.profile.application.mapper;

import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.infrastructure.persistence.entity.ProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "nameChangedAt", source = "nameChangedAt")
    Profile toDomain(ProfileEntity entity);

    ProfileEntity toEntity(Profile domain);
}
