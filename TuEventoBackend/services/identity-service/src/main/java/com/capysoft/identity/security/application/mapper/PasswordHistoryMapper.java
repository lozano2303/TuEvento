package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.PasswordHistory;
import com.capysoft.identity.security.infrastructure.persistence.entity.PasswordHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PasswordHistoryMapper {

    PasswordHistory toDomain(PasswordHistoryEntity entity);
    PasswordHistoryEntity toEntity(PasswordHistory domain);
}
