package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ThemeTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ThemeTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ThemeTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ThemeTranslationRepositoryImpl implements ThemeTranslationRepository {

    private final ThemeTranslationJpaRepository jpaRepository;
    private final ThemeTranslationMapper mapper;

    @Autowired
    public ThemeTranslationRepositoryImpl(ThemeTranslationJpaRepository jpaRepository, 
                                         ThemeTranslationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ThemeTranslation> findByThemeAndLanguage(Integer themeId, Long languageId) {
        return jpaRepository.findByThemeIdAndLanguageId(themeId, languageId.intValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<ThemeTranslation> findByTheme(Integer themeId) {
        return jpaRepository.findByThemeId(themeId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ThemeTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ThemeTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findByStatus(status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ThemeTranslation> findByThemeAndStatus(Integer themeId, TranslationStatus status) {
        // This method would need a custom query in JpaRepository, for now filtering in memory
        return findByTheme(themeId)
                .stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public ThemeTranslation save(ThemeTranslation themeTranslation) {
        var entity = mapper.toEntity(themeTranslation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ThemeTranslation> saveAll(List<ThemeTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ThemeTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByTheme(Integer themeId) {
        var entities = jpaRepository.findByThemeId(themeId);
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByThemeAndLanguage(Integer themeId, Long languageId) {
        return jpaRepository.findByThemeIdAndLanguageId(themeId, languageId.intValue()).isPresent();
    }
}