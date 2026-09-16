package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.AccountReactivationToken;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountReactivationTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AccountReactivationTokenMapper {

    AccountReactivationToken toDomain(AccountReactivationTokenEntity entity);
    AccountReactivationTokenEntity toEntity(AccountReactivationToken domain);
}
