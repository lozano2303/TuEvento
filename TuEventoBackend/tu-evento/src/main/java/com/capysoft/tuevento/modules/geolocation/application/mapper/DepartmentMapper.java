package com.capysoft.tuevento.modules.geolocation.application.mapper;

import org.mapstruct.Mapper;

import com.capysoft.tuevento.modules.geolocation.domain.model.Department;
import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.DepartmentEntity;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    Department toDomain(DepartmentEntity entity);
    DepartmentEntity toEntity(Department domain);
}
