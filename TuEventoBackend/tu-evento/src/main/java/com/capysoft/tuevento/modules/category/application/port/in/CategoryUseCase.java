package com.capysoft.tuevento.modules.category.application.port.in;

import com.capysoft.tuevento.modules.category.application.dto.request.CreateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.request.UpdateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryUseCase {

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deactivateCategory(Long id);

    void activateCategory(Long id);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getActiveCategories();

    List<CategoryResponse> getSubcategories(Long parentId);

    List<CategoryResponse> getRootCategories();
}
