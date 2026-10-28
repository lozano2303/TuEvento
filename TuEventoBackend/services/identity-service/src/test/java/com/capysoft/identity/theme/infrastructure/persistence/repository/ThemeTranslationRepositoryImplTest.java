package com.capysoft.identity.theme.infrastructure.persistence.repository;

import com.capysoft.identity.theme.domain.model.ThemeTranslation;
import com.capysoft.identity.theme.infrastructure.persistence.entity.ThemeTranslationEntity;
import com.capysoft.identity.theme.infrastructure.persistence.mapper.ThemeTranslationMapper;
import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Adaptador de persistencia de traducciones de temas con el mapper real y el repositorio JPA simulado.
 */
@DisplayName("ThemeTranslationRepositoryImpl")
class ThemeTranslationRepositoryImplTest {

    private final ThemeTranslationJpaRepository jpa = mock(ThemeTranslationJpaRepository.class);
    private final ThemeTranslationRepositoryImpl repository =
            new ThemeTranslationRepositoryImpl(jpa, new ThemeTranslationMapper());

    @Test
    @DisplayName("busca por tema e idioma y mapea todos los campos")
    void findsByThemeAndLanguage() {
        when(jpa.findByThemeIdAndLanguageId(3, 2)).thenReturn(Optional.of(ThemeTranslationEntity.builder()
                .translationId(30).themeId(3).languageId(2).translatedName("Ocean")
                .translatedDescription("Blue tones").source(TranslationSource.MANUAL)
                .status(TranslationStatus.PUBLISHED).build()));
        when(jpa.findByThemeIdAndLanguageId(4, 2)).thenReturn(Optional.empty());

        ThemeTranslation found = repository.findByThemeAndLanguage(3, 2).orElseThrow();
        assertThat(found.getTranslationId()).isEqualTo(30);
        assertThat(found.getThemeId()).isEqualTo(3);
        assertThat(found.getLanguageId()).isEqualTo(2);
        assertThat(found.getTranslatedName()).isEqualTo("Ocean");
        assertThat(found.getTranslatedDescription()).isEqualTo("Blue tones");
        assertThat(found.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
        assertThat(repository.findByThemeAndLanguage(4, 2)).isEmpty();
    }
}
