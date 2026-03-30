package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface LoginCredentialsMapper {

    LoginCredentials toDomain(LoginCredentialsEntity entity);
    LoginCredentialsEntity toEntity(LoginCredentials domain);
}
