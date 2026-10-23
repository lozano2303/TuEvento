package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.port.in.DeleteEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingDeletedEvent;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteEventRatingService")
class DeleteEventRatingServiceTest {

    @Mock private EventRatingRepository     ratingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Clock                     clock;

    @InjectMocks private DeleteEventRatingService service;

    private static final Long EVENT_ID  = 10L;
    private static final Long RATING_ID = 42L;
    private static final Long USER_ID   = 7L;
    private static final Long OTHER_USER = 8L;

    private static final Instant FIXED_NOW = Instant.parse("2026-10-05T14:30:00Z");
    private static final ZoneId  BOGOTA    = ZoneId.of("America/Bogota");

    private void givenClock() {
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(clock.getZone()).thenReturn(BOGOTA);
    }

    private EventRating principalRating() {
        return EventRating.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(USER_ID)
                .rating(4).comment("ok").isVisible(true)
                .createdAt(LocalDateTime.now())
                .parentRatingId(null)   // principal
                .build();
    }

    private EventRating replyRating() {
        return EventRating.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(USER_ID)
                .rating(null).comment("reply").isVisible(true)
                .createdAt(LocalDateTime.now())
                .parentRatingId(99L)   // respuesta
                .build();
    }

    @Test @DisplayName("borrar comentario principal borra también sus respuestas")
    void delete_principal_also_deletes_replies() {
        givenClock();
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(principalRating()));

        service.execute(EVENT_ID, RATING_ID, USER_ID);

        verify(ratingRepository).deleteAllByParentRatingId(RATING_ID);
        verify(ratingRepository).deleteById(RATING_ID);
    }

    @Test @DisplayName("borrar una respuesta NO borra el comentario padre")
    void delete_reply_does_not_delete_parent() {
        givenClock();
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(replyRating()));

        service.execute(EVENT_ID, RATING_ID, USER_ID);

        verify(ratingRepository, never()).deleteAllByParentRatingId(any());
        verify(ratingRepository).deleteById(RATING_ID);
    }

    @Test @DisplayName("rating no encontrado → NotFoundException RATING_NOT_FOUND")
    void rating_not_found() {
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, USER_ID))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("RATING_NOT_FOUND"));
    }

    @Test @DisplayName("rating de otro evento → NotFoundException RATING_NOT_FOUND")
    void rating_different_event() {
        EventRating wrongEvent = EventRating.builder()
                .ratingId(RATING_ID).eventId(99L).userId(USER_ID)
                .rating(3).comment("otro").isVisible(true).createdAt(LocalDateTime.now())
                .build();
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(wrongEvent));
        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, USER_ID))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("RATING_NOT_FOUND"));
    }

    @Test @DisplayName("otro usuario intenta borrar → BusinessException RATING_ACCESS_DENIED")
    void other_user_cannot_delete() {
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(principalRating()));
        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OTHER_USER))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("RATING_ACCESS_DENIED"));
        verify(ratingRepository, never()).deleteById(any());
    }

    @Test @DisplayName("evento de dominio publicado con datos correctos")
    void domain_event_published() {
        givenClock();
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(principalRating()));

        service.execute(EVENT_ID, RATING_ID, USER_ID);

        ArgumentCaptor<EventRatingDeletedEvent> captor = ArgumentCaptor.forClass(EventRatingDeletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getRatingId()).isEqualTo(RATING_ID);
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Nested @DisplayName("Orden de operaciones")
    class OrderOfOperations {

        @Test @DisplayName("respuestas se borran ANTES del comentario principal (FK constraint)")
        void replies_deleted_before_principal() {
            givenClock();
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(principalRating()));

            service.execute(EVENT_ID, RATING_ID, USER_ID);

            // Verificar que deleteAll se invocó antes que deleteById
            var inOrder = inOrder(ratingRepository);
            inOrder.verify(ratingRepository).deleteAllByParentRatingId(RATING_ID);
            inOrder.verify(ratingRepository).deleteById(RATING_ID);
        }
    }
}
