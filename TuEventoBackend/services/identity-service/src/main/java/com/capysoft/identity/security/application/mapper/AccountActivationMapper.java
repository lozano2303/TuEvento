package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.AccountActivation;
import com.capysoft.identity.security.infrastructure.persistence.entity.AccountActivationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AccountActivationMapper {

    AccountActivation toDomain(AccountActivationEntity entity);
    AccountActivationEntity toEntity(AccountActivation domain);
}
