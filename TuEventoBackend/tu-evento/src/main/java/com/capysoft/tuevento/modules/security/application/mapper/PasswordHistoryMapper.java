package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.PasswordHistory;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.PasswordHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface PasswordHistoryMapper {

    PasswordHistory toDomain(PasswordHistoryEntity entity);
    PasswordHistoryEntity toEntity(PasswordHistory domain);
}
