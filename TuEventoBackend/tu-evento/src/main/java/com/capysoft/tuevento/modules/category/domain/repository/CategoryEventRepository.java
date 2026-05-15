package com.capysoft.tuevento.modules.category.domain.repository;

import com.capysoft.tuevento.modules.category.domain.model.CategoryEvent;

import java.util.List;
import java.util.Optional;

public interface CategoryEventRepository {

    CategoryEvent save(CategoryEvent categoryEvent);

    Optional<CategoryEvent> findById(Integer id);

    List<CategoryEvent> findByEventId(Integer eventId);

    List<CategoryEvent> findByCategoryId(Integer categoryId);

    boolean existsByCategoryIdAndEventId(Integer categoryId, Integer eventId);

    void deleteById(Integer id);

    void deleteByEventId(Integer eventId);
}
