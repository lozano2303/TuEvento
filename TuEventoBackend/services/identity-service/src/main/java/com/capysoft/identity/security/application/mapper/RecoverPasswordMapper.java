package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.RecoverPassword;
import com.capysoft.identity.security.infrastructure.persistence.entity.RecoverPasswordEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface RecoverPasswordMapper {

    RecoverPassword toDomain(RecoverPasswordEntity entity);
    RecoverPasswordEntity toEntity(RecoverPassword domain);
}
