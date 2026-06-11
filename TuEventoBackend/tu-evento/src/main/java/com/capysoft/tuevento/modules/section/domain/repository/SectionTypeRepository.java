package com.capysoft.tuevento.modules.section.domain.repository;

import com.capysoft.tuevento.modules.section.domain.model.SectionType;

import java.util.List;
import java.util.Optional;

public interface SectionTypeRepository {
    List<SectionType> findAll();
    Optional<SectionType> findById(Integer sectionTypeId);
    boolean existsByName(String name);
    SectionType save(SectionType sectionType);
}
