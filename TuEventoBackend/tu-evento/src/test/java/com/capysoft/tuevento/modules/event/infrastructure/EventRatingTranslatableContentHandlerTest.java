package com.capysoft.tuevento.modules.event.infrastructure;

import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.infrastructure.translation.EventRatingTranslatableContentHandler;
import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Guardado de la traducción de un comentario de evento. */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventRatingTranslatableContentHandler — saveTranslation")
class EventRatingTranslatableContentHandlerTest {

    @Mock private EventRatingRepository            eventRatingRepository;
    @Mock private EventRatingTranslationRepository eventRatingTranslationRepository;
    @Mock private LanguageCatalog                  languageCatalog;

    @InjectMocks private EventRatingTranslatableContentHandler handler;

    @Test
    @DisplayName("Crea la traducción nueva con el comentario traducido y el estado")
    void creates_new_translation() {
        when(languageCatalog.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(eventRatingTranslationRepository.findByRatingAndLanguage(15, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(15L, "en", Map.of("comment", "Great event"),
                TranslationSource.MANUAL, TranslationStatus.PUBLISHED);

        ArgumentCaptor<EventRatingTranslation> captor = ArgumentCaptor.forClass(EventRatingTranslation.class);
        verify(eventRatingTranslationRepository).save(captor.capture());
        EventRatingTranslation saved = captor.getValue();
        assertEquals(15, saved.getRatingId());
        assertEquals(2, saved.getLanguageId());
        assertEquals("Great event", saved.getTranslatedComment());
        assertEquals(TranslationStatus.PUBLISHED, saved.getStatus());
    }

    @Test
    @DisplayName("Idioma desconocido: no guarda nada")
    void unknown_language_is_ignored() {
        when(languageCatalog.findByCode("xx")).thenReturn(Optional.empty());

        handler.saveTranslation(15L, "xx", Map.of("comment", "?"), TranslationSource.MANUAL, TranslationStatus.DRAFT);

        verify(eventRatingTranslationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Estado de la traducción: el guardado, o vacío si no hay traducción o el idioma no existe")
    void translation_status() {
        EventRatingTranslation existing = new EventRatingTranslation();
        existing.setStatus(TranslationStatus.PUBLISHED);
        when(languageCatalog.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languageCatalog.findByCode("xx")).thenReturn(Optional.empty());
        when(eventRatingTranslationRepository.findByRatingAndLanguage(15, 2)).thenReturn(Optional.of(existing));
        when(eventRatingTranslationRepository.findByRatingAndLanguage(16, 2)).thenReturn(Optional.empty());

        assertEquals(Optional.of(TranslationStatus.PUBLISHED), handler.findTranslationStatus(15L, "en"));
        assertEquals(Optional.empty(), handler.findTranslationStatus(16L, "en"));
        assertEquals(Optional.empty(), handler.findTranslationStatus(15L, "xx"));
    }
}
