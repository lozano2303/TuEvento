package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountActivationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AccountActivationMapper {

    AccountActivation toDomain(AccountActivationEntity entity);
    AccountActivationEntity toEntity(AccountActivation domain);
}
