package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.SectionTypeTranslationMapper;
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
    public SectionTypeTranslation save(SectionTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<SectionTypeTranslation> findById(Integer id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<SectionTypeTranslation> findBySectionTypeId(Integer sectionTypeId) {
        return jpaRepository.findBySectionTypeId(sectionTypeId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SectionTypeTranslation> findBySectionTypeIdAndLanguageId(Integer sectionTypeId, Integer languageId) {
        return jpaRepository.findBySectionTypeIdAndLanguageId(sectionTypeId, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<SectionTypeTranslation> findByLanguageId(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(SectionTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<SectionTypeTranslation> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}