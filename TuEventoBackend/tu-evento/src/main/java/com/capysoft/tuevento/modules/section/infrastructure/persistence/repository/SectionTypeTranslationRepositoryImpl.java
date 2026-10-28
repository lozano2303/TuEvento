package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.modules.section.infrastructure.persistence.mapper.SectionTypeTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SectionTypeTranslationRepositoryImpl implements SectionTypeTranslationRepository {

    private final SectionTypeTranslationJpaRepository jpaRepository;
    private final SectionTypeTranslationMapper mapper;

    @Override
    public Optional<SectionTypeTranslation> findBySectionTypeAndLanguage(Long sectionTypeId, Integer languageId) {
        return jpaRepository.findBySectionTypeIdAndLanguageId(sectionTypeId.intValue(), languageId)
                .map(mapper::toDomain);
    }

    @Override
    public SectionTypeTranslation save(SectionTypeTranslation translation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(translation)));
    }

    @Override
    public void delete(SectionTypeTranslation translation) {
        jpaRepository.delete(mapper.toEntity(translation));
    }
}
