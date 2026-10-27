package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeTranslation;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeTranslationRepository;
import com.capysoft.tuevento.modules.theme.infrastructure.persistence.mapper.ThemeTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ThemeTranslationRepositoryImpl implements ThemeTranslationRepository {

    private final ThemeTranslationJpaRepository jpaRepository;
    private final ThemeTranslationMapper mapper;

    @Override
    public Optional<ThemeTranslation> findByThemeAndLanguage(Integer themeId, Integer languageId) {
        return jpaRepository.findByThemeIdAndLanguageId(themeId, languageId).map(mapper::toDomain);
    }
}
