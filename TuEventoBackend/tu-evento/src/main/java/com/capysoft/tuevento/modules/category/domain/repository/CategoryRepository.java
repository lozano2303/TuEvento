package com.capysoft.tuevento.modules.category.domain.repository;

import com.capysoft.tuevento.modules.category.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAll();

    List<Category> findAllActive();

    /** Returns all subcategories whose parent is {@code dadId}. */
    List<Category> findByDadId(Long dadId);

    /** Returns all root categories (dadId IS NULL). */
    List<Category> findRootCategories();

    boolean existsByName(String name);

    boolean existsById(Long id);

    void deleteById(Long id);
}
