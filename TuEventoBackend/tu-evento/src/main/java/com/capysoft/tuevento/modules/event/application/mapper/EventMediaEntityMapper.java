package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventMedia;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventMediaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMediaEntityMapper {

    EventMedia toDomain(EventMediaEntity entity);
    EventMediaEntity toEntity(EventMedia domain);
}
