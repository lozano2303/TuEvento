package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.OauthAccount;
import com.capysoft.identity.security.infrastructure.persistence.entity.OauthAccountEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OauthAccountMapper {

    OauthAccount toDomain(OauthAccountEntity entity);
    OauthAccountEntity toEntity(OauthAccount domain);
}
