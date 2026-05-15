package com.capysoft.tuevento.modules.category.application.mapper;

import com.capysoft.tuevento.modules.category.application.dto.response.CategoryEventResponse;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.model.CategoryEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    @Mapping(target = "categoryName", source = "categoryName")
    CategoryEventResponse toCategoryEventResponse(CategoryEvent categoryEvent, String categoryName);
}
