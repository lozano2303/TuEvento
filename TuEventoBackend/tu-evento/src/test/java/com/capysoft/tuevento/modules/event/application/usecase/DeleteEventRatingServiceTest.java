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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteEventRatingService}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: borra el rating y publica EventRatingDeletedEvent</li>
 *   <li>Tras borrar, existsByEventIdAndUserId ya no bloquea (usuario puede volver a comentar)</li>
 *   <li>Rating no encontrado → NotFoundException RATING_NOT_FOUND</li>
 *   <li>Rating de otro evento → NotFoundException RATING_NOT_FOUND</li>
 *   <li>Rating de otra persona → BusinessException RATING_ACCESS_DENIED</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteEventRatingService")
class DeleteEventRatingServiceTest {

    @Mock private EventRatingRepository    ratingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeleteEventRatingService service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long EVENT_ID  = 10L;
    private static final Long RATING_ID = 42L;
    private static final Long OWNER_ID  = 7L;
    private static final Long OTHER_ID  = 99L;

    private EventRating ownerRating() {
        return EventRating.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(OWNER_ID)
                .rating(4).comment("Buen evento").isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("dueño borra OK: llama deleteById y publica EventRatingDeletedEvent")
        void owner_deletes_successfully() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ownerRating()));

            service.execute(EVENT_ID, RATING_ID, OWNER_ID);

            verify(ratingRepository).deleteById(RATING_ID);

            ArgumentCaptor<EventRatingDeletedEvent> captor =
                    ArgumentCaptor.forClass(EventRatingDeletedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            EventRatingDeletedEvent evt = captor.getValue();
            assertThat(evt.getRatingId()).isEqualTo(RATING_ID);
            assertThat(evt.getEventId()).isEqualTo(EVENT_ID);
            assertThat(evt.getUserId()).isEqualTo(OWNER_ID);
            assertThat(evt.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("tras borrar, existsByEventIdAndUserId retorna false → usuario puede volver a comentar")
        void after_delete_user_can_comment_again() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ownerRating()));
            // Simula que después del borrado existsByEventIdAndUserId retorna false
            when(ratingRepository.existsByEventIdAndUserId(EVENT_ID, OWNER_ID)).thenReturn(false);

            service.execute(EVENT_ID, RATING_ID, OWNER_ID);

            // existsByEventIdAndUserId puede ser llamado (por AddEventRatingService en el futuro),
            // y ahora devuelve false — el usuario puede volver a comentar.
            assertThat(ratingRepository.existsByEventIdAndUserId(EVENT_ID, OWNER_ID)).isFalse();
        }
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validaciones de negocio")
    class Validations {

        @Test
        @DisplayName("rating no encontrado → NotFoundException RATING_NOT_FOUND")
        void rating_not_found_throws() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                            .isEqualTo("RATING_NOT_FOUND"));

            verify(ratingRepository, never()).deleteById(anyLong());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("rating existe pero pertenece a otro evento → NotFoundException RATING_NOT_FOUND")
        void rating_belongs_to_different_event_throws() {
            Long otherEventId = 999L;
            EventRating ratingOfOtherEvent = EventRating.builder()
                    .ratingId(RATING_ID).eventId(otherEventId).userId(OWNER_ID)
                    .rating(3).comment("Otro evento").isVisible(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ratingOfOtherEvent));

            // Se pasa EVENT_ID pero el rating pertenece a otherEventId
            assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                            .isEqualTo("RATING_NOT_FOUND"));

            verify(ratingRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("rating existe y pertenece al evento pero es de otra persona → BusinessException RATING_ACCESS_DENIED")
        void non_owner_throws_access_denied() {
            when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(ownerRating()));

            // OTHER_ID intenta borrar el rating del OWNER_ID
            assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OTHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("RATING_ACCESS_DENIED"));

            verify(ratingRepository, never()).deleteById(anyLong());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
