package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AuthSessionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthSessionMapper {

    AuthSession toDomain(AuthSessionEntity entity);
    AuthSessionEntity toEntity(AuthSession domain);
}
