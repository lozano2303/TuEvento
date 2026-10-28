package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.User;
import com.capysoft.identity.security.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserStatusMapper.class, RoleMapper.class})
public interface UserMapper {

    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);
}
