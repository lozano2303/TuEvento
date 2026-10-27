package com.capysoft.identity.security.application.mapper;

import com.capysoft.identity.security.domain.model.Permission;
import com.capysoft.identity.security.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "roles", ignore = true)
    PermissionEntity toEntity(Permission domain);

    Permission toDomain(PermissionEntity entity);
}
