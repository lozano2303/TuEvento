package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CategoryEventJpaRepository extends JpaRepository<CategoryEventEntity, Integer> {

    List<CategoryEventEntity> findByEventId(Integer eventId);

    List<CategoryEventEntity> findByCategoryId(Integer categoryId);

    boolean existsByCategoryIdAndEventId(Integer categoryId, Integer eventId);

    @Transactional
    void deleteByEventId(Integer eventId);
}
