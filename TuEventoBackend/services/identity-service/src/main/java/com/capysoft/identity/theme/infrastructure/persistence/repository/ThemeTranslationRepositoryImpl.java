package com.capysoft.identity.theme.infrastructure.persistence.repository;

import com.capysoft.identity.theme.domain.model.ThemeTranslation;
import com.capysoft.identity.theme.domain.repository.ThemeTranslationRepository;
import com.capysoft.identity.theme.infrastructure.persistence.mapper.ThemeTranslationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
    public Optional<ThemeTranslation> findByThemeAndLanguage(Integer themeId, Integer languageId) {
        return jpaRepository.findByThemeIdAndLanguageId(themeId, languageId)
                .map(mapper::toDomain);
    }
}
