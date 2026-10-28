package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.RefreshToken;
import com.capysoft.identity.security.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class, AuthSessionMapper.class})
public interface RefreshTokenMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);
    RefreshTokenEntity toEntity(RefreshToken domain);
}
