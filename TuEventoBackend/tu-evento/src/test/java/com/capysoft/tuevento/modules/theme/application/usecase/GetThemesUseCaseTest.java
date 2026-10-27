package com.capysoft.tuevento.modules.theme.application.usecase;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeResponse;
import com.capysoft.tuevento.modules.theme.application.mapper.ThemeAppMapper;
import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeTranslation;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeRepository;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeTranslationRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los temas se devuelven en español y, en otro idioma, con su traducción publicada si existe.
 */
@DisplayName("GetThemesUseCase")
class GetThemesUseCaseTest {

    private static final Theme OCEANO = Theme.builder().id(1).name("Océano").description("Tonos azules").build();
    private static final Theme BOSQUE = Theme.builder().id(2).name("Bosque").description("Tonos verdes").build();

    private final ThemeRepository themes = mock(ThemeRepository.class);
    private final ThemeAppMapper mapper = mock(ThemeAppMapper.class);
    private final LanguageRepository languages = mock(LanguageRepository.class);
    private final ThemeTranslationRepository translations = mock(ThemeTranslationRepository.class);
    private final GetThemesUseCase useCase = new GetThemesUseCase(themes, mapper, languages, translations);

    private static ThemeTranslation translation(String name, TranslationStatus status) {
        ThemeTranslation t = new ThemeTranslation();
        t.setTranslatedName(name);
        t.setTranslatedDescription(name + " tones");
        t.setStatus(status);
        return t;
    }

    @BeforeEach
    void data() {
        when(themes.findAll()).thenReturn(List.of(OCEANO, BOSQUE));
        when(mapper.toThemeResponse(any())).thenAnswer(inv -> {
            Theme theme = inv.getArgument(0);
            return ThemeResponse.builder().id(theme.getId()).name(theme.getName()).build();
        });
        when(languages.findByCode("en")).thenReturn(Optional.of(Language.builder().languageId(2).code("en").build()));
    }

    @Test
    @DisplayName("sin idioma, en español o con un idioma desconocido no traduce")
    void untranslated() {
        when(languages.findByCode("xx")).thenReturn(Optional.empty());

        assertThat(useCase.getAll()).extracting(ThemeResponse::getName).containsExactly("Océano", "Bosque");
        assertThat(useCase.getAll(" ")).hasSize(2);
        assertThat(useCase.getAll("ES")).hasSize(2);
        assertThat(useCase.getAll("xx")).extracting(ThemeResponse::getTranslatedName).containsOnlyNulls();
        verify(translations, never()).findByThemeAndLanguage(any(), any());
    }

    @Test
    @DisplayName("usa solo traducciones publicadas; un fallo deja el tema sin traducir")
    void translated() {
        when(translations.findByThemeAndLanguage(1, 2))
                .thenReturn(Optional.of(translation("Ocean", TranslationStatus.PUBLISHED)));
        when(translations.findByThemeAndLanguage(2, 2))
                .thenReturn(Optional.of(translation("Forest", TranslationStatus.DRAFT)));

        List<ThemeResponse> result = useCase.getAll("EN");
        assertThat(result.get(0).getTranslatedName()).isEqualTo("Ocean");
        assertThat(result.get(0).getTranslatedDescription()).isEqualTo("Ocean tones");
        assertThat(result.get(0).getLanguageCode()).isEqualTo("EN");
        assertThat(result.get(1).getTranslatedName()).isNull();
        assertThat(result.get(1).getName()).isEqualTo("Bosque");

        when(translations.findByThemeAndLanguage(1, 2)).thenThrow(new IllegalStateException("bd caída"));
        assertThat(useCase.getAll("en").get(0).getTranslatedName()).isNull();
    }
}
