package com.capysoft.tuevento.modules.event.application.listener;

import com.capysoft.tuevento.modules.event.domain.event.EventUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.repository.EventLogRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Al editar un evento, sus traducciones solo se invalidan si cambió el nombre o la descripción.
 */
@DisplayName("EventTranslationListener")
class EventTranslationListenerTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 27, 12, 0);

    private final EventTranslationRepository translations = mock(EventTranslationRepository.class);
    private final TranslationRequests jobs = mock(TranslationRequests.class);
    private final EventLogRepository logs = mock(EventLogRepository.class);
    private final EventTranslationListener listener = new EventTranslationListener(translations, jobs, logs);

    private static EventUpdatedEvent updated() {
        return EventUpdatedEvent.builder().eventId(7L).userId(1).occurredAt(AT).build();
    }

    @Test
    @DisplayName("si cambió el nombre o la descripción borra traducciones y trabajos pendientes")
    void contentChanged() {
        when(logs.existsByEventIdAndActionInAndOccurredAtBetween(eq(7L),
                eq(List.of("EVENT_NAME_CHANGED", "DESCRIPTION_CHANGED")), eq(AT.minusSeconds(5)), eq(AT.plusSeconds(5))))
                .thenReturn(true);

        listener.onEventUpdated(updated());

        verify(translations).deleteByEvent(7);
        verify(jobs).discard("event", 7);
    }

    @Test
    @DisplayName("si no cambió el contenido traducible conserva las traducciones")
    void contentUnchanged() {
        when(logs.existsByEventIdAndActionInAndOccurredAtBetween(any(), anyList(), any(), any())).thenReturn(false);

        listener.onEventUpdated(updated());

        verify(translations, never()).deleteByEvent(anyInt());
        verify(jobs, never()).discard(anyString(), anyLong());
    }

    @Test
    @DisplayName("si no puede leer el historial asume que cambió, y un fallo al borrar no se propaga")
    void failures() {
        when(logs.existsByEventIdAndActionInAndOccurredAtBetween(any(), anyList(), any(), any()))
                .thenThrow(new IllegalStateException("bd caída"));
        doThrow(new IllegalStateException("bd caída")).when(translations).deleteByEvent(7);

        assertThatCode(() -> listener.onEventUpdated(updated())).doesNotThrowAnyException();
        verify(translations).deleteByEvent(7);
    }
}
