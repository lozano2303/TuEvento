package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.AuthSession;
import com.capysoft.identity.security.infrastructure.persistence.entity.AuthSessionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthSessionMapper {

    AuthSession toDomain(AuthSessionEntity entity);
    AuthSessionEntity toEntity(AuthSession domain);
}
