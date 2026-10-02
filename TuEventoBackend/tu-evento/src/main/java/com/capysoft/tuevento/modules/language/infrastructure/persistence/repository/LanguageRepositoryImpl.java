package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.LanguageInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación JPA del repositorio de dominio LanguageRepository.
 */
@Repository
@RequiredArgsConstructor
public class LanguageRepositoryImpl implements LanguageRepository {

    private final LanguageJpaRepository jpaRepository;
    private final LanguageInfraMapper mapper;

    @Override
    public Optional<Language> findById(Long languageId) {
        return jpaRepository.findById(languageId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Language> findByCode(String code) {
        return jpaRepository.findByCode(code)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Language> findDefaultLanguage() {
        return jpaRepository.findByIsDefaultTrue()
                .map(mapper::toDomain);
    }

    @Override
    public List<Language> findAllActive() {
        List<LanguageEntity> entities = jpaRepository.findByIsActiveTrueOrderByName();
        return mapper.toDomainList(entities);
    }

    @Override
    public List<Language> findAll() {
        List<LanguageEntity> entities = jpaRepository.findAllOrderByName();
        return mapper.toDomainList(entities);
    }

    @Override
    public Language save(Language language) {
        LanguageEntity entity = mapper.toEntity(language);
        LanguageEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByCodeIgnoreCase(String code) {
        return jpaRepository.existsByCodeIgnoreCase(code);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }
}