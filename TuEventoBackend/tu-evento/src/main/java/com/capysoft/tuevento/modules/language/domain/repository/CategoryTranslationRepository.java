package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.CategoryTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para CategoryTranslation.
 */
public interface CategoryTranslationRepository {

    /**
     * Busca una traducción específica por categoría y idioma.
     */
    Optional<CategoryTranslation> findByCategoryAndLanguage(Integer categoryId, Long languageId);

    /**
     * Busca todas las traducciones de una categoría.
     */
    List<CategoryTranslation> findByCategory(Integer categoryId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<CategoryTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<CategoryTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por categoría y estado.
     */
    List<CategoryTranslation> findByCategoryAndStatus(Integer categoryId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    CategoryTranslation save(CategoryTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<CategoryTranslation> saveAll(List<CategoryTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(CategoryTranslation translation);

    /**
     * Elimina todas las traducciones de una categoría.
     */
    void deleteByCategory(Integer categoryId);

    /**
     * Verifica si existe una traducción para categoría y idioma específicos.
     */
    boolean existsByCategoryAndLanguage(Integer categoryId, Long languageId);
}