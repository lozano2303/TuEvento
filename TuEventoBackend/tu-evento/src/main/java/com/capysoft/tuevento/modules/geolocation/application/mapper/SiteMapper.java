package com.capysoft.tuevento.modules.geolocation.application.mapper;

import org.mapstruct.Mapper;

import com.capysoft.tuevento.modules.geolocation.domain.model.Site;
import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.SiteEntity;

@Mapper(componentModel = "spring", uses = {CityMapper.class})
public interface SiteMapper {

    Site toDomain(SiteEntity entity);
    SiteEntity toEntity(Site domain);
}
