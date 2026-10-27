package com.capysoft.tuevento.modules.category.domain.repository;

import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;

import java.util.Optional;

/**
 * Repositorio de dominio para CategoryTranslation.
 */
public interface CategoryTranslationRepository {

    /**
     * Busca una traducción específica por categoría y idioma.
     */
    Optional<CategoryTranslation> findByCategoryAndLanguage(Integer categoryId, Integer languageId);

    /**
     * Guarda una traducción: la crea si no tiene id o actualiza la existente.
     */
    CategoryTranslation save(CategoryTranslation translation);

    /**
     * Elimina una traducción.
     */
    void delete(CategoryTranslation translation);
}
