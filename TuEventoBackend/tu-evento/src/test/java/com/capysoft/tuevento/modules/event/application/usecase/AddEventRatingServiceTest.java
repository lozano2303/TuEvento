package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.AddEventRatingRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AddEventRatingService}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: guarda el rating, publica el evento de dominio y retorna response con authorName</li>
 *   <li>Evento no encontrado → NotFoundException EVENT_NOT_FOUND</li>
 *   <li>Evento en estado inválido (DRAFT) → BusinessException EVENT_RATING_NOT_ALLOWED</li>
 *   <li>Evento privado (isPublic=false) → BusinessException EVENT_RATING_NOT_ALLOWED</li>
 *   <li>Usuario ya calificó el evento → BusinessException EVENT_ALREADY_RATED</li>
 *   <li>authorName cae a "Usuario" cuando no hay perfil</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddEventRatingService")
class AddEventRatingServiceTest {

    @Mock private EventRepository          eventRepository;
    @Mock private EventRatingRepository    ratingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProfileJpaRepository     profileJpaRepository;

    @InjectMocks
    private AddEventRatingService service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long     EVENT_ID = 10L;
    private static final Long     USER_ID  = 7L;
    private static final LocalDate FUTURE  = LocalDate.now().plusDays(5);

    private Event publishedPublicEvent() {
        return Event.builder()
                .eventId(EVENT_ID).userId(99L).siteId(1L)
                .eventName("Rock Fest").description("Desc")
                .startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                .status(EventStatus.PUBLISHED).isPublic(true).availableSeats(200)
                .build();
    }

    private AddEventRatingRequest validRequest() {
        return AddEventRatingRequest.builder()
                .rating(4)
                .comment("Gran evento, lo recomiendo")
                .build();
    }

    private EventRating savedRating() {
        return EventRating.builder()
                .ratingId(1L).eventId(EVENT_ID).userId(USER_ID)
                .rating(4).comment("Gran evento, lo recomiendo")
                .isVisible(true).createdAt(LocalDateTime.now())
                .build();
    }

    // ── Tests: happy path ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("guarda el rating, publica EventRatingAddedEvent y retorna response completo")
        void saves_publishes_and_returns_response() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.existsByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRating());

            ProfileEntity profile = mock(ProfileEntity.class);
            when(profile.getFullName()).thenReturn("Ana García");
            when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.of(profile));

            EventRatingResponse response = service.execute(EVENT_ID, validRequest(), USER_ID);

            // Verifica respuesta REST
            assertThat(response.getRatingId()).isEqualTo(1L);
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getAuthorName()).isEqualTo("Ana García");
            assertThat(response.getRating()).isEqualTo(4);
            assertThat(response.getComment()).isEqualTo("Gran evento, lo recomiendo");
            assertThat(response.getIsVisible()).isTrue();
            assertThat(response.getCreatedAt()).isNotNull();

            // Verifica que el repositorio guardó el rating
            verify(ratingRepository).save(argThat(r ->
                    r.getEventId().equals(EVENT_ID)
                    && r.getUserId().equals(USER_ID)
                    && r.getRating() == 4
                    && "Gran evento, lo recomiendo".equals(r.getComment())
                    && Boolean.TRUE.equals(r.getIsVisible())
            ));

            // Verifica que el evento de dominio fue publicado con todos los campos nuevos
            ArgumentCaptor<EventRatingAddedEvent> eventCaptor =
                    ArgumentCaptor.forClass(EventRatingAddedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            EventRatingAddedEvent domainEvent = eventCaptor.getValue();
            assertThat(domainEvent.getRatingId()).isEqualTo(1L);
            assertThat(domainEvent.getEventId()).isEqualTo(EVENT_ID);
            assertThat(domainEvent.getUserId()).isEqualTo(USER_ID);
            assertThat(domainEvent.getComment()).isEqualTo("Gran evento, lo recomiendo");
            assertThat(domainEvent.getIsVisible()).isTrue();
        }

        @Test
        @DisplayName("authorName cae a 'Usuario' cuando el usuario no tiene perfil")
        void authorName_fallback_when_no_profile() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.existsByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRating());
            when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.empty());

            EventRatingResponse response = service.execute(EVENT_ID, validRequest(), USER_ID);

            assertThat(response.getAuthorName()).isEqualTo("Usuario");
        }

        @Test
        @DisplayName("también acepta evento en estado COMPLETED")
        void completed_event_is_allowed() {
            Event completedEvent = Event.builder()
                    .eventId(EVENT_ID).userId(99L).siteId(1L)
                    .eventName("Rock Fest").description("Desc")
                    .startDate(FUTURE.minusDays(10)).finishDate(FUTURE.minusDays(9))
                    .status(EventStatus.COMPLETED).isPublic(true).availableSeats(200)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(completedEvent));
            when(ratingRepository.existsByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRating());
            when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, validRequest(), USER_ID));
        }
    }

    // ── Tests: validaciones ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Validaciones de negocio")
    class BusinessValidations {

        @Test
        @DisplayName("evento no encontrado → NotFoundException EVENT_NOT_FOUND")
        void event_not_found_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EVENT_ID, validRequest(), USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                            .isEqualTo("EVENT_NOT_FOUND"));

            verify(ratingRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("evento en estado DRAFT → BusinessException EVENT_RATING_NOT_ALLOWED")
        void draft_event_throws() {
            Event draftEvent = Event.builder()
                    .eventId(EVENT_ID).userId(99L).siteId(1L)
                    .eventName("Draft").description("Desc")
                    .startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                    .status(EventStatus.DRAFT).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draftEvent));

            assertThatThrownBy(() -> service.execute(EVENT_ID, validRequest(), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_RATING_NOT_ALLOWED"));

            verify(ratingRepository, never()).save(any());
        }

        @Test
        @DisplayName("evento privado (isPublic=false) → BusinessException EVENT_RATING_NOT_ALLOWED")
        void private_event_throws() {
            Event privateEvent = Event.builder()
                    .eventId(EVENT_ID).userId(99L).siteId(1L)
                    .eventName("Private").description("Desc")
                    .startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                    .status(EventStatus.PUBLISHED).isPublic(false).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(privateEvent));

            assertThatThrownBy(() -> service.execute(EVENT_ID, validRequest(), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_RATING_NOT_ALLOWED"));

            verify(ratingRepository, never()).save(any());
        }

        @Test
        @DisplayName("usuario ya calificó el evento → BusinessException EVENT_ALREADY_RATED")
        void already_rated_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.existsByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(EVENT_ID, validRequest(), USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_ALREADY_RATED"));

            verify(ratingRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
