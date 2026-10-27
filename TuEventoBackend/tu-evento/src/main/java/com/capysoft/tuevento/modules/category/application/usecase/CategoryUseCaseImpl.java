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
import com.capysoft.tuevento.modules.language.application.service.TranslationService;
import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryUseCaseImpl implements CategoryUseCase {

    private final CategoryRepository         categoryRepository;
    private final CategoryMapper             categoryMapper;
    private final ApplicationEventPublisher  eventPublisher;
    private final LanguageRepository         languageRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final TranslationJobRepository   translationJobRepository;
    private final TranslationService         translationService;

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
    public CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request) {
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

        // Request automatic translations if content changed
        if (nameChanged || (existing.getDescription() != null && !existing.getDescription().equals(request.getDescription()))) {
            try {
                translationService.requestTranslation("category", updated.getCategoryId().longValue(), "es");
                log.debug("Translation jobs requested for updated category: id={}", updated.getCategoryId());
            } catch (Exception e) {
                log.warn("Failed to request translations for updated category {}: {}", updated.getCategoryId(), e.getMessage());
                // Non-blocking: translation failure should not prevent category update
            }
        }

        log.debug("Category updated: id={}", updated.getCategoryId());
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateCategory(Integer id) {
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
    public void activateCategory(Integer id) {
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
    public CategoryResponse getCategoryById(Integer id) {
        return getCategoryById(id, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id, String languageCode) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Category not found: " + id));
        return toResponseWithTranslation(category, languageCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return getAllCategories(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(String languageCode) {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(category -> toResponseWithTranslation(category, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return getActiveCategories(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories(String languageCode) {
        List<Category> categories = categoryRepository.findAllActive();
        return categories.stream()
                .map(category -> toResponseWithTranslation(category, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Integer parentId) {
        return getSubcategories(parentId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Integer parentId, String languageCode) {
        if (!categoryRepository.existsById(parentId)) {
            throw new NotFoundException("CATEGORY_NOT_FOUND",
                    "Category not found: " + parentId);
        }
        List<Category> categories = categoryRepository.findByDadId(parentId);
        return categories.stream()
                .map(category -> toResponseWithTranslation(category, languageCode))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return getRootCategories(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories(String languageCode) {
        List<Category> categories = categoryRepository.findRootCategories();
        return categories.stream()
                .map(category -> toResponseWithTranslation(category, languageCode))
                .collect(Collectors.toList());
    }

    /**
     * Convierte una categoría a response incluyendo traducción si está disponible
     */
    private CategoryResponse toResponseWithTranslation(Category category, String languageCode) {
        String translatedName = null;
        String translatedDescription = null;
        String actualLanguageCode = null;
        Boolean translationGenerating = false;

        if (languageCode != null && !languageCode.isBlank()) {
            try {
                // IMPORTANT: Don't translate if requested language is the same as source language (Spanish)
                // Categories are created in Spanish by default
                if ("es".equalsIgnoreCase(languageCode)) {
                    // Return original content without translation for Spanish
                    log.debug("Requested language is Spanish (source language) - no translation needed for category {}", 
                            category.getCategoryId());
                } else {
                    var language = languageRepository.findByCode(languageCode);
                    if (language.isPresent()) {
                        var translation = categoryTranslationRepository.findByCategoryAndLanguage(
                                category.getCategoryId(), language.get().getLanguageId());
                        
                        if (translation.isPresent() && translation.get().getStatus() == TranslationStatus.PUBLISHED) {
                            translatedName = translation.get().getTranslatedName();
                            translatedDescription = translation.get().getTranslatedDescription();
                            actualLanguageCode = languageCode;
                        } else {
                            // Translation doesn't exist - check job status before creating
                            log.info("Translation not found for category {} language {} - checking job status",
                                    category.getCategoryId(), languageCode);
                            
                            // Check if job already exists
                            var existingJob = translationJobRepository.findByEntityAndTargetLanguage(
                                    "category", category.getCategoryId(), language.get().getLanguageId());
                            
                            if (existingJob.isEmpty()) {
                                // Create translation job
                                translationService.requestTranslation("category", category.getCategoryId().longValue(), "es");
                                translationGenerating = true;
                                log.info("Lazy loading translation job created for category {} -> {}", 
                                        category.getCategoryId(), languageCode);
                            } else {
                                // Job exists - check its status
                                var jobStatus = existingJob.get().getStatus();
                                if (jobStatus.name().equals("PENDING") || jobStatus.name().equals("PROCESSING")) {
                                    translationGenerating = true;
                                    log.info("Translation job in progress ({}) for category {} -> {}", 
                                            jobStatus, category.getCategoryId(), languageCode);
                                } else if (jobStatus.name().equals("COMPLETED")) {
                                    // Job completed but no PUBLISHED translation found - this shouldn't happen
                                    // but let's handle it gracefully by not showing generating indicator
                                    log.warn("Translation job COMPLETED but no PUBLISHED translation found for category {} -> {}", 
                                            category.getCategoryId(), languageCode);
                                } else if (jobStatus.name().equals("FAILED")) {
                                    // Job failed - could retry or show error, for now don't show generating
                                    log.warn("Translation job FAILED for category {} -> {}", 
                                            category.getCategoryId(), languageCode);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not resolve translation for category {} language {}: {}",
                        category.getCategoryId(), languageCode, ex.getMessage());
            }
        }

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .visible(category.isVisible())
                .dadId(category.getDadId())
                // Translation fields
                .translatedName(translatedName)
                .translatedDescription(translatedDescription)
                .languageCode(actualLanguageCode)
                .translationGenerating(translationGenerating)
                .build();
    }
}
