package com.capysoft.tuevento.modules.category.application.usecase;

import com.capysoft.tuevento.modules.category.application.dto.request.CreateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.request.UpdateCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.mapper.CategoryMapper;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryUseCase;
import com.capysoft.tuevento.modules.category.domain.event.CategoryCreatedEvent;
import com.capysoft.tuevento.modules.category.domain.event.CategoryDeactivatedEvent;
import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryUseCaseImpl implements CategoryUseCase {

    private final CategoryRepository     categoryRepository;
    private final CategoryMapper         categoryMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("CATEGORY_NAME_ALREADY_EXISTS",
                    "A category with name '" + request.getName() + "' already exists");
        }

        if (request.getDadId() != null && !categoryRepository.existsById(request.getDadId())) {
            throw new NotFoundException("PARENT_CATEGORY_NOT_FOUND",
                    "Parent category not found: " + request.getDadId());
        }

        Category saved = categoryRepository.save(Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .visible(true)
                .dadId(request.getDadId())
                .build());

        eventPublisher.publishEvent(CategoryCreatedEvent.builder()
                .categoryId(saved.getCategoryId())
                .name(saved.getName())
                .hasParent(saved.getDadId() != null)
                .build());

        log.debug("Category created: id={}, name={}", saved.getCategoryId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Category not found: " + id));

        boolean nameChanged = !existing.getName().equals(request.getName());
        if (nameChanged && categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("CATEGORY_NAME_ALREADY_EXISTS",
                    "A category with name '" + request.getName() + "' already exists");
        }

        if (request.getDadId() != null && !categoryRepository.existsById(request.getDadId())) {
            throw new NotFoundException("PARENT_CATEGORY_NOT_FOUND",
                    "Parent category not found: " + request.getDadId());
        }

        Category updated = categoryRepository.save(Category.builder()
                .categoryId(existing.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .active(existing.isActive())
                .visible(existing.isVisible())
                .dadId(request.getDadId())
                .build());

        log.debug("Category updated: id={}", updated.getCategoryId());
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateCategory(Long id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Category not found: " + id));

        categoryRepository.save(Category.builder()
                .categoryId(existing.getCategoryId())
                .name(existing.getName())
                .description(existing.getDescription())
                .active(false)
                .visible(existing.isVisible())
                .dadId(existing.getDadId())
                .build());

        eventPublisher.publishEvent(CategoryDeactivatedEvent.builder()
                .categoryId(existing.getCategoryId())
                .name(existing.getName())
                .build());

        log.debug("Category deactivated: id={}", id);
    }

    @Override
    @Transactional
    public void activateCategory(Long id) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Category not found: " + id));

        categoryRepository.save(Category.builder()
                .categoryId(existing.getCategoryId())
                .name(existing.getName())
                .description(existing.getDescription())
                .active(true)
                .visible(existing.isVisible())
                .dadId(existing.getDadId())
                .build());

        log.debug("Category activated: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(
                categoryRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                                "Category not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryMapper.toResponseList(categoryRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryMapper.toResponseList(categoryRepository.findAllActive());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Long parentId) {
        if (!categoryRepository.existsById(parentId)) {
            throw new NotFoundException("CATEGORY_NOT_FOUND",
                    "Category not found: " + parentId);
        }
        return categoryMapper.toResponseList(categoryRepository.findByDadId(parentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryMapper.toResponseList(categoryRepository.findRootCategories());
    }
}
