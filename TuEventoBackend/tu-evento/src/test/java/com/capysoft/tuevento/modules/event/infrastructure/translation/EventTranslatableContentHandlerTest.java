package com.capysoft.tuevento.modules.event.infrastructure.translation;

import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
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

/** Guardado y estado de la traducción de un evento. */
@DisplayName("EventTranslatableContentHandler")
class EventTranslatableContentHandlerTest {

    private final EventTranslationRepository translations = mock(EventTranslationRepository.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final EventTranslatableContentHandler handler =
            new EventTranslatableContentHandler(mock(EventRepository.class), translations, languages);

    @BeforeEach
    void languages() {
        when(languages.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("actualiza la traducción existente con nombre, descripción, origen y estado")
    void updates() {
        EventTranslation existing = EventTranslation.builder().translationId(70).eventId(7).languageId(2).build();
        when(translations.findByEventAndLanguage(7, 2)).thenReturn(Optional.of(existing));

        handler.saveTranslation(7L, "en", Map.of("event_name", "Concert", "description", "Live music"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        ArgumentCaptor<EventTranslation> saved = ArgumentCaptor.forClass(EventTranslation.class);
        verify(translations).save(saved.capture());
        assertThat(saved.getValue()).extracting(EventTranslation::getTranslationId, EventTranslation::getTranslatedName,
                        EventTranslation::getTranslatedDescription, EventTranslation::getSource, EventTranslation::getStatus)
                .containsExactly(70, "Concert", "Live music", TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("sin textos útiles o con idioma desconocido no guarda")
    void skips() {
        when(translations.findByEventAndLanguage(7, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(7L, "en", Map.of("event_name", ""), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        handler.saveTranslation(7L, "xx", Map.of("event_name", "Concert"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        verify(translations, never()).save(any());
    }

    @Test
    @DisplayName("estado de la traducción, o vacío si el idioma no existe")
    void status() {
        when(translations.findByEventAndLanguage(7, 2))
                .thenReturn(Optional.of(EventTranslation.builder().status(TranslationStatus.PUBLISHED).build()));

        assertThat(handler.findTranslationStatus(7L, "en")).contains(TranslationStatus.PUBLISHED);
        assertThat(handler.findTranslationStatus(7L, "xx")).isEmpty();
    }
}
