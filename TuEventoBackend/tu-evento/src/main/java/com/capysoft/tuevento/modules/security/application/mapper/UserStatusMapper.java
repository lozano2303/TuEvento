package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserStatusEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserStatusMapper {

    UserStatus toDomain(UserStatusEntity entity);
    UserStatusEntity toEntity(UserStatus domain);
}
