package com.capysoft.tuevento.modules.language.infrastructure.persistence.jpa;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.CategoryTranslationEntity;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para CategoryTranslationEntity.
 */
@Repository
public interface CategoryTranslationJpaRepository extends JpaRepository<CategoryTranslationEntity, Long> {

    /**
     * Busca traducción por categoría e idioma.
     */
    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId AND ct.language.languageId = :languageId")
    Optional<CategoryTranslationEntity> findByCategoryIdAndLanguageId(@Param("categoryId") Integer categoryId, 
                                                                     @Param("languageId") Long languageId);

    /**
     * Busca todas las traducciones de una categoría.
     */
    List<CategoryTranslationEntity> findByCategoryId(Integer categoryId);

    /**
     * Busca todas las traducciones en un idioma.
     */
    @Query("SELECT ct FROM CategoryTranslationEntity ct WHERE ct.language.languageId = :languageId")
    List<CategoryTranslationEntity> findByLanguageId(@Param("languageId") Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<CategoryTranslationEntity> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por categoría y estado.
     */
    List<CategoryTranslationEntity> findByCategoryIdAndStatus(Integer categoryId, TranslationStatus status);

    /**
     * Elimina traducciones por categoría.
     */
    void deleteByCategoryId(Integer categoryId);

    /**
     * Verifica existencia por categoría e idioma.
     */
    @Query("SELECT COUNT(ct) > 0 FROM CategoryTranslationEntity ct WHERE ct.categoryId = :categoryId AND ct.language.languageId = :languageId")
    boolean existsByCategoryIdAndLanguageId(@Param("categoryId") Integer categoryId, 
                                          @Param("languageId") Long languageId);
}