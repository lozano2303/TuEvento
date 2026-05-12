package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventStatusLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventStatusLogEntityMapper {

    EventStatusLog toDomain(EventStatusLogEntity entity);
    EventStatusLogEntity toEntity(EventStatusLog domain);
}
