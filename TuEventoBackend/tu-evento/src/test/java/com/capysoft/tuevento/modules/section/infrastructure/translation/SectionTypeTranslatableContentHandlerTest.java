package com.capysoft.tuevento.modules.section.infrastructure.translation;

import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Guardado y estado de la traducción de un tipo de sección. */
@DisplayName("SectionTypeTranslatableContentHandler")
class SectionTypeTranslatableContentHandlerTest {

    private final SectionTypeTranslationRepository translations = mock(SectionTypeTranslationRepository.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final SectionTypeTranslatableContentHandler handler =
            new SectionTypeTranslatableContentHandler(mock(SectionTypeRepository.class), translations, languages);

    @BeforeEach
    void languages() {
        when(languages.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("crea la traducción con el nombre, origen y estado")
    void creates() {
        when(translations.findBySectionTypeAndLanguage(3L, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(3L, "en", Map.of("name", "Box"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        ArgumentCaptor<SectionTypeTranslation> saved = ArgumentCaptor.forClass(SectionTypeTranslation.class);
        verify(translations).save(saved.capture());
        assertThat(saved.getValue()).extracting(SectionTypeTranslation::getSectionTypeId, SectionTypeTranslation::getLanguageId,
                        SectionTypeTranslation::getTranslatedName, SectionTypeTranslation::getSource,
                        SectionTypeTranslation::getStatus)
                .containsExactly(3, 2, "Box", TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("sin nombre útil o con idioma desconocido no guarda")
    void skips() {
        when(translations.findBySectionTypeAndLanguage(3L, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(3L, "en", Map.of("name", " "), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        handler.saveTranslation(3L, "xx", Map.of("name", "Box"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        verify(translations, never()).save(any());
    }

    @Test
    @DisplayName("estado de la traducción, o vacío si el idioma no existe")
    void status() {
        SectionTypeTranslation existing = new SectionTypeTranslation();
        existing.setStatus(TranslationStatus.DRAFT);
        when(translations.findBySectionTypeAndLanguage(3L, 2)).thenReturn(Optional.of(existing));

        assertThat(handler.findTranslationStatus(3L, "en")).contains(TranslationStatus.DRAFT);
        assertThat(handler.findTranslationStatus(3L, "xx")).isEmpty();
    }
}
