package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventMediaLog;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventMediaLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMediaLogEntityMapper {

    EventMediaLog toDomain(EventMediaLogEntity entity);
    EventMediaLogEntity toEntity(EventMediaLog domain);
}
