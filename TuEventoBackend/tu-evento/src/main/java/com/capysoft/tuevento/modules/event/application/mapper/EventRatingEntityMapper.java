package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventRatingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventRatingEntityMapper {

    EventRating toDomain(EventRatingEntity entity);
    EventRatingEntity toEntity(EventRating domain);
}
