package com.capysoft.tuevento.modules.category.application.port.in;

import com.capysoft.tuevento.modules.category.application.dto.request.AssignCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryEventResponse;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryEventUseCase {

    CategoryEventResponse assignCategoryToEvent(AssignCategoryRequest request);

    void removeCategoryFromEvent(Long categoryEventId);

    List<CategoryResponse> getCategoriesByEvent(Long eventId);

    List<Long> getEventIdsByCategory(Long categoryId);

    void removeAllCategoriesFromEvent(Long eventId);
}
