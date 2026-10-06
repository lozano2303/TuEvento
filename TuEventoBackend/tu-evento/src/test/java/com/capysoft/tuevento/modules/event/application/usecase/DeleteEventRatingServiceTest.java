package com.capysoft.tuevento.modules.event.application.usecase;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteEventRatingService")
class DeleteEventRatingServiceTest {

    @Mock private EventRatingRepository    ratingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Clock                    clock;

    @InjectMocks
    private DeleteEventRatingService service;

    private static final Long EVENT_ID  = 10L;
    private static final Long RATING_ID = 42L;
    private static final Long OWNER_ID  = 7L;
    private static final Long OTHER_ID  = 99L;

    @SuppressWarnings("all")
    private void setupClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-05T14:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    private EventRating ownerRating(Integer rating) {
        return EventRating.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(OWNER_ID)
                .rating(rating).comment("Buen evento").isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("dueño borra su comentario con rating → deleteById + EventRatingDeletedEvent")
        void owner_deletes_rated_comment() {
            setupClock();
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ownerRating(4)));

            service.execute(EVENT_ID, RATING_ID, OWNER_ID);

            verify(ratingRepository).deleteById(RATING_ID);
            ArgumentCaptor<EventRatingDeletedEvent> cap = ArgumentCaptor.forClass(EventRatingDeletedEvent.class);
            verify(eventPublisher).publishEvent(cap.capture());
            assertThat(cap.getValue().getRatingId()).isEqualTo(RATING_ID);
            assertThat(cap.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(cap.getValue().getUserId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("dueño borra su comentario sin rating (null) → también permitido")
        void owner_deletes_unrated_comment() {
            setupClock();
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ownerRating(null)));

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, RATING_ID, OWNER_ID));
            verify(ratingRepository).deleteById(RATING_ID);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validations {

        @Test
        @DisplayName("rating no encontrado → NotFoundException RATING_NOT_FOUND")
        void not_found_throws() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("RATING_NOT_FOUND"));
            verify(ratingRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("rating de otro evento → NotFoundException RATING_NOT_FOUND")
        void wrong_event_throws() {
            EventRating otherEvent = EventRating.builder()
                    .ratingId(RATING_ID).eventId(999L).userId(OWNER_ID)
                    .rating(3).comment("x").isVisible(true).createdAt(LocalDateTime.now()).build();
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(otherEvent));

            assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("RATING_NOT_FOUND"));
        }

        @Test
        @DisplayName("rating de otra persona → BusinessException RATING_ACCESS_DENIED")
        void non_owner_throws() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ownerRating(4)));

            assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OTHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("RATING_ACCESS_DENIED"));
            verify(ratingRepository, never()).deleteById(anyLong());
        }
    }
}
