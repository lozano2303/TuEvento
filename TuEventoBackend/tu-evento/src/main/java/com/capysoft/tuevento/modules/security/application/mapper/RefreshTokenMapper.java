package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.RefreshToken;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class, AuthSessionMapper.class})
public interface RefreshTokenMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);
    RefreshTokenEntity toEntity(RefreshToken domain);
}
