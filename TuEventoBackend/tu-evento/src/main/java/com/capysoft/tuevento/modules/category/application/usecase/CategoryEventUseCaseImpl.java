package com.capysoft.tuevento.modules.category.application.usecase;

import com.capysoft.tuevento.modules.category.application.dto.request.AssignCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryEventResponse;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.mapper.CategoryMapper;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.category.domain.event.CategoryAssignedToEventEvent;
import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.model.CategoryEvent;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryEventRepository;
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
public class CategoryEventUseCaseImpl implements CategoryEventUseCase {

    private final CategoryRepository      categoryRepository;
    private final CategoryEventRepository categoryEventRepository;
    private final CategoryMapper          categoryMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CategoryEventResponse assignCategoryToEvent(AssignCategoryRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                        "Category not found: " + request.getCategoryId()));

        if (!category.isActive()) {
            throw new BusinessException("CATEGORY_INACTIVE",
                    "Category '" + category.getName() + "' is not active");
        }

        if (categoryEventRepository.existsByCategoryIdAndEventId(
                request.getCategoryId(), request.getEventId())) {
            throw new BusinessException("CATEGORY_EVENT_ALREADY_EXISTS",
                    "Category " + request.getCategoryId()
                    + " is already assigned to event " + request.getEventId());
        }

        CategoryEvent saved = categoryEventRepository.save(CategoryEvent.builder()
                .categoryId(request.getCategoryId())
                .eventId(request.getEventId())
                .build());

        eventPublisher.publishEvent(CategoryAssignedToEventEvent.builder()
                .categoryEventId(saved.getCategoryEventId())
                .categoryId(saved.getCategoryId())
                .eventId(saved.getEventId())
                .build());

        log.debug("Category {} assigned to event {}", saved.getCategoryId(), saved.getEventId());
        return categoryMapper.toCategoryEventResponse(saved, category.getName());
    }

    @Override
    @Transactional
    public void removeCategoryFromEvent(Long categoryEventId) {
        if (!categoryEventRepository.findById(categoryEventId).isPresent()) {
            throw new NotFoundException("CATEGORY_EVENT_NOT_FOUND",
                    "CategoryEvent not found: " + categoryEventId);
        }
        categoryEventRepository.deleteById(categoryEventId);
        log.debug("CategoryEvent removed: id={}", categoryEventId);
    }

    @Override
    @Transactional
    public void removeAllCategoriesFromEvent(Long eventId) {
        categoryEventRepository.deleteByEventId(eventId);
        log.debug("All categories removed from event: id={}", eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByEvent(Long eventId) {
        List<CategoryEvent> categoryEvents = categoryEventRepository.findByEventId(eventId);

        List<Category> categories = categoryEvents.stream()
                .map(ce -> categoryRepository.findById(ce.getCategoryId())
                        .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND",
                                "Category not found: " + ce.getCategoryId())))
                .toList();

        return categoryMapper.toResponseList(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getEventIdsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("CATEGORY_NOT_FOUND",
                    "Category not found: " + categoryId);
        }
        return categoryEventRepository.findByCategoryId(categoryId)
                .stream()
                .map(CategoryEvent::getEventId)
                .toList();
    }
}
