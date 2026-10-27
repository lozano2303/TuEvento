package com.capysoft.tuevento.modules.section.domain.repository;

import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;

import java.util.Optional;

/**
 * Repositorio de dominio para las traducciones de tipos de sección.
 */
public interface SectionTypeTranslationRepository {

    Optional<SectionTypeTranslation> findBySectionTypeAndLanguage(Long sectionTypeId, Integer languageId);

    SectionTypeTranslation save(SectionTypeTranslation translation);

    void delete(SectionTypeTranslation translation);
}
