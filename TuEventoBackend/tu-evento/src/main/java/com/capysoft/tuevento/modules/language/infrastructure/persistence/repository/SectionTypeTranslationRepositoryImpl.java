package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.SectionTypeTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<SectionTypeTranslation> findBySectionType(Long sectionTypeId) {
        return jpaRepository.findBySectionTypeId(sectionTypeId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SectionTypeTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SectionTypeTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<SectionTypeTranslation> findBySectionTypeAndStatus(Long sectionTypeId, TranslationStatus status) {
        return jpaRepository.findBySectionTypeId(sectionTypeId.intValue())
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public SectionTypeTranslation save(SectionTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<SectionTypeTranslation> saveAll(List<SectionTypeTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(SectionTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteBySectionType(Long sectionTypeId) {
        var entities = jpaRepository.findBySectionTypeId(sectionTypeId.intValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsBySectionTypeAndLanguage(Long sectionTypeId, Integer languageId) {
        return jpaRepository.findBySectionTypeIdAndLanguageId(sectionTypeId.intValue(), languageId)
                .isPresent();
    }
}