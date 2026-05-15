package com.capysoft.tuevento.modules.category.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.category.domain.model.CategoryEvent;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryEventEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryEventEntityMapper {

    CategoryEventEntity toEntity(CategoryEvent domain);

    CategoryEvent toDomain(CategoryEventEntity entity);

    List<CategoryEvent> toDomainList(List<CategoryEventEntity> entities);
}
