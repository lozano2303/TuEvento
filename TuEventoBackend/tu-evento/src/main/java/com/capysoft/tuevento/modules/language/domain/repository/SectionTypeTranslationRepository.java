package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.SectionTypeTranslation;

import java.util.List;
import java.util.Optional;

public interface SectionTypeTranslationRepository {

    SectionTypeTranslation save(SectionTypeTranslation translation);

    Optional<SectionTypeTranslation> findById(Integer id);

    List<SectionTypeTranslation> findBySectionTypeId(Integer sectionTypeId);

    Optional<SectionTypeTranslation> findBySectionTypeIdAndLanguageId(Integer sectionTypeId, Integer languageId);

    List<SectionTypeTranslation> findByLanguageId(Integer languageId);

    void delete(SectionTypeTranslation translation);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    List<SectionTypeTranslation> findAll();
}