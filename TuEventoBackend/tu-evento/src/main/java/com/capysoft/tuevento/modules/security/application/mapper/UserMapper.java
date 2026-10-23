package com.capysoft.tuevento.modules.security.application.mapper;

import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserStatusMapper.class, RoleMapper.class})
public interface UserMapper {

    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);
}
