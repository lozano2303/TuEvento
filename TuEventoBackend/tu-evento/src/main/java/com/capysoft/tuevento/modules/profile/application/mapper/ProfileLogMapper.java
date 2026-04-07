package com.capysoft.tuevento.modules.profile.application.mapper;

import com.capysoft.tuevento.modules.profile.domain.model.ProfileLog;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileLogMapper {

    ProfileLog toDomain(ProfileLogEntity entity);
    ProfileLogEntity toEntity(ProfileLog domain);
}
