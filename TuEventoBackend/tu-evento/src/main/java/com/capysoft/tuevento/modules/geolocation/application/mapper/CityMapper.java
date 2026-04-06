package com.capysoft.tuevento.modules.geolocation.application.mapper;

import org.mapstruct.Mapper;

import com.capysoft.tuevento.modules.geolocation.domain.model.City;
import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.CityEntity;

@Mapper(componentModel = "spring", uses = {DepartmentMapper.class})
public interface CityMapper {

    City toDomain(CityEntity entity);
    CityEntity toEntity(City domain);
}
