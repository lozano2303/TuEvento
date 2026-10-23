package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.OauthAccount;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OauthAccountEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface OauthAccountMapper {

    OauthAccount toDomain(OauthAccountEntity entity);
    OauthAccountEntity toEntity(OauthAccount domain);
}
