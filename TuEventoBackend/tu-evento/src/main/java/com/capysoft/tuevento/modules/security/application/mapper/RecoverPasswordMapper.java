package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.RecoverPassword;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RecoverPasswordEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface RecoverPasswordMapper {

    RecoverPassword toDomain(RecoverPasswordEntity entity);
    RecoverPasswordEntity toEntity(RecoverPassword domain);
}
