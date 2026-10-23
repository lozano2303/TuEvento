package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventLayout;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventLayoutEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventLayoutEntityMapper {

    EventLayout toDomain(EventLayoutEntity entity);
    EventLayoutEntity toEntity(EventLayout domain);
}
