package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.Role;
import com.capysoft.identity.security.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    Role toDomain(RoleEntity entity);
    RoleEntity toEntity(Role domain);
}
