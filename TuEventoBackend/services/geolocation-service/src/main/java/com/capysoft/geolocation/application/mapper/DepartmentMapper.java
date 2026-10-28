package com.capysoft.geolocation.application.mapper;

import org.mapstruct.Mapper;

import com.capysoft.geolocation.domain.model.Department;
import com.capysoft.geolocation.infrastructure.persistence.entity.DepartmentEntity;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    Department toDomain(DepartmentEntity entity);
    DepartmentEntity toEntity(Department domain);
}
