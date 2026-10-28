package com.capysoft.identity.theme.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.identity.theme.application.dto.response.ThemeResponse;
import com.capysoft.identity.theme.application.mapper.ThemeAppMapper;
import com.capysoft.identity.theme.application.port.out.LanguageDirectoryPort;
import com.capysoft.identity.theme.domain.model.Theme;
import com.capysoft.identity.theme.domain.model.ThemeTranslation;
import com.capysoft.identity.theme.domain.repository.ThemeRepository;
import com.capysoft.identity.theme.domain.repository.ThemeTranslationRepository;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;

@DisplayName("GetThemesUseCase — traducciones de los temas")
class GetThemesUseCaseTest {

    private final ThemeRepository themes = mock(ThemeRepository.class);
    private final ThemeAppMapper mapper = mock(ThemeAppMapper.class);
    private final LanguageDirectoryPort languages = mock(LanguageDirectoryPort.class);
    private final ThemeTranslationRepository translations = mock(ThemeTranslationRepository.class);
    private final GetThemesUseCase useCase = new GetThemesUseCase(themes, mapper, languages, translations);

    private final Theme dark = Theme.builder().id(1).name("Oscuro").description("Tema oscuro").build();
    private final Theme light = Theme.builder().id(2).name("Claro").description("Tema claro").build();

    @Test
    @DisplayName("usa solo las traducciones publicadas")
    void appliesOnlyPublishedTranslations() {
        when(themes.findAll()).thenReturn(List.of(dark, light));
        when(languages.findIdByCode("en")).thenReturn(Optional.of(2));
        when(translations.findByThemeAndLanguage(1, 2)).thenReturn(Optional.of(translation(TranslationStatus.PUBLISHED)));
        when(translations.findByThemeAndLanguage(2, 2)).thenReturn(Optional.of(translation(TranslationStatus.DRAFT)));

        List<ThemeResponse> result = useCase.getAll("EN");

        assertThat(result.get(0).getTranslatedName()).isEqualTo("Dark");
        assertThat(result.get(0).getLanguageCode()).isEqualTo("EN");
        assertThat(result.get(1).getTranslatedName()).isNull();
        assertThat(result.get(1).getName()).isEqualTo("Claro");
    }

    @Test
    @DisplayName("con un idioma desconocido o en español devuelve los temas sin traducir")
    void unknownOrSourceLanguage() {
        when(themes.findAll()).thenReturn(List.of(dark));
        when(mapper.toThemeResponse(any())).thenReturn(ThemeResponse.builder().id(1).name("Oscuro").build());
        when(languages.findIdByCode("xx")).thenReturn(Optional.empty());

        assertThat(useCase.getAll("xx")).extracting(ThemeResponse::getName).containsExactly("Oscuro");
        assertThat(useCase.getAll("es")).extracting(ThemeResponse::getName).containsExactly("Oscuro");
        verifyNoInteractions(translations);
    }

    private static ThemeTranslation translation(TranslationStatus status) {
        ThemeTranslation t = new ThemeTranslation();
        t.setTranslatedName("Dark");
        t.setTranslatedDescription("Dark theme");
        t.setStatus(status);
        return t;
    }
}
