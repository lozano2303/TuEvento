package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.UserStatus;
import com.capysoft.identity.security.infrastructure.persistence.entity.UserStatusEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserStatusMapper {

    UserStatus toDomain(UserStatusEntity entity);
    UserStatusEntity toEntity(UserStatus domain);
}
