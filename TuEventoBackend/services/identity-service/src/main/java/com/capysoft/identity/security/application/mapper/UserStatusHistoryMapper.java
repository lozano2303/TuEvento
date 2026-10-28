package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.UserStatusHistory;
import com.capysoft.identity.security.infrastructure.persistence.entity.UserStatusHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class, UserStatusMapper.class})
public interface UserStatusHistoryMapper {

    UserStatusHistory toDomain(UserStatusHistoryEntity entity);
    UserStatusHistoryEntity toEntity(UserStatusHistory domain);
}
