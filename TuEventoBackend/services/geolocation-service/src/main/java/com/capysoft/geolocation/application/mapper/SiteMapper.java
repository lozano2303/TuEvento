package com.capysoft.geolocation.application.mapper;

import org.mapstruct.Mapper;

import com.capysoft.geolocation.domain.model.Site;
import com.capysoft.geolocation.infrastructure.persistence.entity.SiteEntity;

@Mapper(componentModel = "spring", uses = {CityMapper.class})
public interface SiteMapper {

    Site toDomain(SiteEntity entity);
    SiteEntity toEntity(Site domain);
}
