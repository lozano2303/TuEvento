package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByActiveTrue();

    List<CategoryEntity> findByDadId(Long dadId);

    List<CategoryEntity> findByDadIdIsNull();

    boolean existsByName(String name);
}
