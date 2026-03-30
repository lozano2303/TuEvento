package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountLockoutEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AccountLockoutMapper {

    AccountLockout toDomain(AccountLockoutEntity entity);
    AccountLockoutEntity toEntity(AccountLockout domain);
}
