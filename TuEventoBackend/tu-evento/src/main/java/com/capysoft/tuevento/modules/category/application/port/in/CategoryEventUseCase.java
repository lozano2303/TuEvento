package com.capysoft.tuevento.modules.category.application.port.in;

import com.capysoft.tuevento.modules.category.application.dto.request.AssignCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryEventResponse;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryEventUseCase {

    CategoryEventResponse assignCategoryToEvent(AssignCategoryRequest request);

    void removeCategoryFromEvent(Integer categoryEventId);

    List<CategoryResponse> getCategoriesByEvent(Integer eventId);

    List<Integer> getEventIdsByCategory(Integer categoryId);

    void removeAllCategoriesFromEvent(Integer eventId);
}
