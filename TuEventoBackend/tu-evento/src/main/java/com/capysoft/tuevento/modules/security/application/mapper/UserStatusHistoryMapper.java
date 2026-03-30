package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.UserStatusHistory;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserStatusHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class, UserStatusMapper.class})
public interface UserStatusHistoryMapper {

    UserStatusHistory toDomain(UserStatusHistoryEntity entity);
    UserStatusHistoryEntity toEntity(UserStatusHistory domain);
}
