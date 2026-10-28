package com.capysoft.identity.profile.application.mapper;

import com.capysoft.identity.profile.domain.model.ProfileLog;
import com.capysoft.identity.profile.infrastructure.persistence.entity.ProfileLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileLogMapper {

    ProfileLog toDomain(ProfileLogEntity entity);
    ProfileLogEntity toEntity(ProfileLog domain);
}
