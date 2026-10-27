package com.capysoft.tuevento.shared.infrastructure.translation;

import com.capysoft.tuevento.shared.application.translation.TranslatableContentHandler;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El registro de contenido traducible encuentra el handler de cada tipo de entidad, le pide el
 * texto original y guarda las traducciones automáticas sin pisar una ya publicada.
 */
@DisplayName("TranslatableContents")
class TranslatableContentsTest {

    private final TranslatableContentHandler events = mock(TranslatableContentHandler.class);
    private TranslatableContents contents;

    @BeforeEach
    void setUp() {
        when(events.entityType()).thenReturn("event");
        contents = new TranslatableContents(List.of(events));
    }

    @Test
    @DisplayName("el texto original lo da el handler del tipo; un tipo desconocido no tiene texto")
    void sourceTexts() {
        when(events.loadSourceTexts(7L)).thenReturn(Map.of("event_name", "Concierto"));

        assertThat(contents.sourceTexts("event", 7L)).containsEntry("event_name", "Concierto");
        assertThat(contents.sourceTexts("planet", 7L)).isEmpty();
    }

    @Test
    @DisplayName("guarda la traducción automática como publicada")
    void savesMachineTranslation() {
        when(events.findTranslationStatus(7L, "en")).thenReturn(Optional.empty());

        assertThat(contents.saveMachineTranslation("event", 7L, "en", Map.of("event_name", "Concert"))).isTrue();

        verify(events).saveTranslation(7L, "en", Map.of("event_name", "Concert"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("un borrador se reemplaza; una traducción publicada o un tipo desconocido no")
    void keepsPublishedTranslation() {
        when(events.findTranslationStatus(7L, "fr")).thenReturn(Optional.of(TranslationStatus.DRAFT));
        when(events.findTranslationStatus(7L, "en")).thenReturn(Optional.of(TranslationStatus.PUBLISHED));

        assertThat(contents.saveMachineTranslation("event", 7L, "fr", Map.of("event_name", "Concert"))).isTrue();
        assertThat(contents.saveMachineTranslation("event", 7L, "en", Map.of("event_name", "Concert"))).isFalse();
        assertThat(contents.saveMachineTranslation("planet", 7L, "en", Map.of())).isFalse();

        verify(events).saveTranslation(eq(7L), eq("fr"), anyMap(), any(), any());
        verify(events, never()).saveTranslation(eq(7L), eq("en"), anyMap(), any(), any());
    }
}
