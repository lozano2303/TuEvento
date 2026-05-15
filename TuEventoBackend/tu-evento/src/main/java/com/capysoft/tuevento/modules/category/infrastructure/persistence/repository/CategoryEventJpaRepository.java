package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CategoryEventJpaRepository extends JpaRepository<CategoryEventEntity, Long> {

    List<CategoryEventEntity> findByEventId(Long eventId);

    List<CategoryEventEntity> findByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndEventId(Long categoryId, Long eventId);

    @Transactional
    void deleteByEventId(Long eventId);
}
