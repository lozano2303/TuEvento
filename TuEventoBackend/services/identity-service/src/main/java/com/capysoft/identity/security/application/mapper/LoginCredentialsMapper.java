package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.LoginCredentials;
import com.capysoft.identity.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface LoginCredentialsMapper {

    LoginCredentials toDomain(LoginCredentialsEntity entity);
    LoginCredentialsEntity toEntity(LoginCredentials domain);
}
