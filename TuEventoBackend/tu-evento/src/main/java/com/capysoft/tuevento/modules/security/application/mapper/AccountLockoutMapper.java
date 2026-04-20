package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountLockoutEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AccountLockoutMapper {

    @Mapping(target = "accountLockoutId", source = "accountLockoutId")
    AccountLockout toDomain(AccountLockoutEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "accountLockoutId", source = "accountLockoutId")
    AccountLockoutEntity toEntity(AccountLockout domain);
}
