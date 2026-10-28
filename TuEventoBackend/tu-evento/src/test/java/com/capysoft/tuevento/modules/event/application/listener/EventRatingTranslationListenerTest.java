package com.capysoft.tuevento.modules.event.application.listener;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Crear, editar o borrar un comentario invalida sus traducciones y trabajos pendientes.
 */
@DisplayName("EventRatingTranslationListener")
class EventRatingTranslationListenerTest {

    private final EventRatingTranslationRepository translations = mock(EventRatingTranslationRepository.class);
    private final TranslationRequests jobs = mock(TranslationRequests.class);
    private final EventRatingTranslationListener listener = new EventRatingTranslationListener(translations, jobs);

    @Test
    @DisplayName("alta, edición y borrado borran traducciones y trabajos del comentario")
    void invalidatesOnEveryChange() {
        listener.onEventRatingAdded(EventRatingAddedEvent.builder().ratingId(4L).build());
        listener.onEventRatingUpdated(EventRatingUpdatedEvent.builder().ratingId(4L).build());
        listener.onEventRatingDeleted(EventRatingDeletedEvent.builder().ratingId(4L).build());

        verify(translations, times(3)).deleteByRating(4);
        verify(jobs, times(3)).discard("event_rating", 4);
    }

    @Test
    @DisplayName("un fallo al borrar no se propaga")
    void failureIsSwallowed() {
        doThrow(new IllegalStateException("bd caída")).when(translations).deleteByRating(4);

        assertThatCode(() -> listener.onEventRatingUpdated(EventRatingUpdatedEvent.builder().ratingId(4L).build()))
                .doesNotThrowAnyException();
    }
}
