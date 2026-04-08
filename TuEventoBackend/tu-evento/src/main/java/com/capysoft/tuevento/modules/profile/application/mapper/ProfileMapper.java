package com.capysoft.tuevento.modules.profile.application.mapper;

import com.capysoft.tuevento.modules.geolocation.application.mapper.CityMapper;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CityMapper.class})
public interface ProfileMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Profile toDomain(ProfileEntity entity);

    ProfileEntity toEntity(Profile domain);
}
