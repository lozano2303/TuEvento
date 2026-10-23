package com.capysoft.tuevento.modules.category.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryEntityMapper {

    CategoryEntity toEntity(Category domain);

    Category toDomain(CategoryEntity entity);

    List<Category> toDomainList(List<CategoryEntity> entities);
}
