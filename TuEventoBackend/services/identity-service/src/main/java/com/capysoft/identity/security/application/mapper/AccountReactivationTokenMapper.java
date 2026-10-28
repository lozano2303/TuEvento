package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.AccountReactivationToken;
import com.capysoft.identity.security.infrastructure.persistence.entity.AccountReactivationTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AccountReactivationTokenMapper {

    AccountReactivationToken toDomain(AccountReactivationTokenEntity entity);
    AccountReactivationTokenEntity toEntity(AccountReactivationToken domain);
}
