package com.capysoft.tuevento.modules.category.domain.repository;

import com.capysoft.tuevento.modules.category.domain.model.CategoryEvent;

import java.util.List;
import java.util.Optional;

public interface CategoryEventRepository {

    CategoryEvent save(CategoryEvent categoryEvent);

    Optional<CategoryEvent> findById(Long id);

    List<CategoryEvent> findByEventId(Long eventId);

    List<CategoryEvent> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndEventId(Long categoryId, Long eventId);

    void deleteById(Long id);

    void deleteByEventId(Long eventId);
}
