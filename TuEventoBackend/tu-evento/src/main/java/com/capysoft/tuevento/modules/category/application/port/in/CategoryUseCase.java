package com.capysoft.tuevento.modules.category.application.port.in;

import com.capysoft.tuevento.modules.category.application.dto.request.CreateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.request.UpdateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryUseCase {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request);

    void deactivateCategory(Integer id);

    void activateCategory(Integer id);

    CategoryResponse getCategoryById(Integer id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getActiveCategories();

    List<CategoryResponse> getSubcategories(Integer parentId);

    List<CategoryResponse> getRootCategories();
}
