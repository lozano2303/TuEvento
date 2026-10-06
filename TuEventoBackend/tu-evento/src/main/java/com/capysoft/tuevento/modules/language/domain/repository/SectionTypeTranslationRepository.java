package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para SectionTypeTranslation.
 */
public interface SectionTypeTranslationRepository {

    /**
     * Busca una traducción específica por section type y idioma.
     */
    Optional<SectionTypeTranslation> findBySectionTypeAndLanguage(Long sectionTypeId, Long languageId);

    /**
     * Busca todas las traducciones de un section type.
     */
    List<SectionTypeTranslation> findBySectionType(Long sectionTypeId);

    /**
     * Busca todas las traducciones en un idioma específico.
     */
    List<SectionTypeTranslation> findByLanguage(Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<SectionTypeTranslation> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por section type y estado.
     */
    List<SectionTypeTranslation> findBySectionTypeAndStatus(Long sectionTypeId, TranslationStatus status);

    /**
     * Guarda una traducción.
     */
    SectionTypeTranslation save(SectionTypeTranslation translation);

    /**
     * Guarda múltiples traducciones.
     */
    List<SectionTypeTranslation> saveAll(List<SectionTypeTranslation> translations);

    /**
     * Elimina una traducción.
     */
    void delete(SectionTypeTranslation translation);

    /**
     * Elimina todas las traducciones de un section type.
     */
    void deleteBySectionType(Long sectionTypeId);

    /**
     * Verifica si existe una traducción para section type y idioma específicos.
     */
    boolean existsBySectionTypeAndLanguage(Long sectionTypeId, Long languageId);
}