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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AddEventRatingService} with rules R1-R5.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddEventRatingService")
class AddEventRatingServiceTest {

    @Mock private EventRepository          eventRepository;
    @Mock private EventRatingRepository    ratingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProfileJpaRepository     profileJpaRepository;
    @Mock private Clock                    clock;

    @InjectMocks
    private AddEventRatingService service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long     EVENT_ID    = 10L;
    private static final Long     ORGANIZER   = 1L;   // userId del dueño del evento
    private static final Long     USER_A      = 7L;
    private static final Long     USER_B      = 8L;
    private static final Long     ORGANIZER_USER = 99L; // un usuario con rol ORGANIZER distinto al dueño
    private static final Long     ADMIN_USER  = 100L;
    private static final LocalDate FUTURE      = LocalDate.now().plusDays(5);

    private static final Instant FIXED_NOW =
            Instant.parse("2026-10-05T14:30:00Z");
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Evento publicado y público, dueño = ORGANIZER (userId=1). */
    private Event publishedPublicEvent() {
        return Event.builder()
                .eventId(EVENT_ID).userId(ORGANIZER).siteId(1L)
                .eventName("Rock Fest").description("Desc")
                .startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                .status(EventStatus.PUBLISHED).isPublic(true).availableSeats(200)
                .build();
    }

    private AddEventRatingRequest requestWith(Integer rating, String comment) {
        return AddEventRatingRequest.builder().rating(rating).comment(comment).build();
    }

    private EventRating savedRatingWith(Long userId, Integer rating, String comment, LocalDateTime createdAt) {
        return EventRating.builder()
                .ratingId(42L).eventId(EVENT_ID).userId(userId)
                .rating(rating).comment(comment)
                .isVisible(true).createdAt(createdAt)
                .build();
    }

    private static final LocalDateTime FIXED_NOW_LDT =
            LocalDateTime.ofInstant(FIXED_NOW, BOGOTA);

    /** Prepara el Clock fijo. Llamar al inicio de cada test que llegue al bloque de guardado. */
    private void givenClock() {
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(clock.getZone()).thenReturn(BOGOTA);
    }

    private void stubNoProfile(Long userId) {
        when(profileJpaRepository.findByUserId(userId.intValue())).thenReturn(Optional.empty());
    }

    private void stubProfile(Long userId, String name) {
        ProfileEntity p = new ProfileEntity();
        p.setFullName(name);
        p.setUserId(userId.intValue());
        when(profileJpaRepository.findByUserId(userId.intValue())).thenReturn(Optional.of(p));
    }

    private LocalDateTime now() {
        return FIXED_NOW_LDT;
    }

    // ── R3: primer comentario con rating ─────────────────────────────────────

    @Nested
    @DisplayName("R3 — lógica de calificación")
    class RatingLogic {

        @Test
        @DisplayName("primer comentario sin rating previo → se guarda con el rating enviado")
        void first_comment_saves_rating() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(Optional.empty());
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, 4, "Muy bueno", now()));
            stubProfile(USER_A, "Ana García");

            EventRatingResponse res = service.execute(EVENT_ID, requestWith(4, "Muy bueno"), USER_A);

            assertThat(res.getRating()).isEqualTo(4);

            ArgumentCaptor<EventRating> captor = ArgumentCaptor.forClass(EventRating.class);
            verify(ratingRepository).save(captor.capture());
            assertThat(captor.getValue().getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("primer comentario pero sin rating enviado → se guarda con rating null")
        void first_comment_no_rating_sent_saves_null() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(Optional.empty());
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, null, "Interesante", now()));
            stubProfile(USER_A, "Ana");

            EventRatingResponse res = service.execute(EVENT_ID, requestWith(null, "Interesante"), USER_A);

            assertThat(res.getRating()).isNull();
        }

        @Test
        @DisplayName("segundo comentario (ya tiene uno con rating) → se guarda con rating null aunque envíe 5")
        void second_comment_ignores_rating_and_saves_null() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            LocalDateTime prevTime = now().minusSeconds(30);
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A))
                    .thenReturn(Optional.of(savedRatingWith(USER_A, 4, "Anterior", prevTime)));
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(true);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, null, "Otro comentario", now()));
            stubProfile(USER_A, "Ana");

            EventRatingResponse res = service.execute(EVENT_ID, requestWith(5, "Otro comentario"), USER_A);

            assertThat(res.getRating()).isNull();
            ArgumentCaptor<EventRating> captor = ArgumentCaptor.forClass(EventRating.class);
            verify(ratingRepository).save(captor.capture());
            assertThat(captor.getValue().getRating()).isNull();
        }

        @Test
        @DisplayName("dos usuarios distintos — cada uno tiene su primer comentario con rating")
        void two_users_each_gets_their_own_first_rated_comment() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));

            // USER_A — primer comentario
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(Optional.empty());
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(false);
            when(ratingRepository.save(argThat(r -> USER_A.equals(r.getUserId()))))
                    .thenReturn(savedRatingWith(USER_A, 5, "Excelente", now()));
            stubProfile(USER_A, "Ana");
            EventRatingResponse resA = service.execute(EVENT_ID, requestWith(5, "Excelente"), USER_A);
            assertThat(resA.getRating()).isEqualTo(5);

            // USER_B — primer comentario en el mismo evento
            reset(ratingRepository, profileJpaRepository, eventPublisher);
            givenClock();
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_B)).thenReturn(Optional.empty());
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_B)).thenReturn(false);
            when(ratingRepository.save(argThat(r -> USER_B.equals(r.getUserId()))))
                    .thenReturn(savedRatingWith(USER_B, 3, "Regular", now()));
            stubProfile(USER_B, "Bob");
            EventRatingResponse resB = service.execute(EVENT_ID, requestWith(3, "Regular"), USER_B);
            assertThat(resB.getRating()).isEqualTo(3);
        }
    }

    // ── R4: organizador no califica ───────────────────────────────────────────

    @Nested
    @DisplayName("R4 — el dueño del evento nunca califica")
    class OrganizerRule {

        @Test
        @DisplayName("organizador envía rating 5 → se guarda null, isOrganizer=true")
        void organizer_rating_is_always_null() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, ORGANIZER)).thenReturn(Optional.empty());
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(ORGANIZER, null, "Mi evento", now()));
            stubProfile(ORGANIZER, "Carlos");

            EventRatingResponse res = service.execute(EVENT_ID, requestWith(5, "Mi evento"), ORGANIZER);

            assertThat(res.getRating()).isNull();
            assertThat(res.getIsOrganizer()).isTrue();

            ArgumentCaptor<EventRating> captor = ArgumentCaptor.forClass(EventRating.class);
            verify(ratingRepository).save(captor.capture());
            assertThat(captor.getValue().getRating()).isNull();
        }

        @Test
        @DisplayName("organizador no consulta existsRatedComment (R4 se aplica antes de R3)")
        void organizer_skips_r3_check() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, ORGANIZER)).thenReturn(Optional.empty());
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(ORGANIZER, null, "Hola", now()));
            stubNoProfile(ORGANIZER);

            service.execute(EVENT_ID, requestWith(4, "Hola"), ORGANIZER);

            verify(ratingRepository, never()).existsRatedCommentByEventIdAndUserId(anyLong(), anyLong());
        }
    }

    // ── R5: antispam ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R5 — antispam 10 segundos")
    class Antispam {

        @Test
        @DisplayName("comentario a 5 segundos del anterior → COMMENT_RATE_LIMITED")
        void within_window_throws_rate_limited() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            // Último comentario hace 5 segundos
            LocalDateTime fiveSecondsAgo = now().minusSeconds(5);
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A))
                    .thenReturn(Optional.of(savedRatingWith(USER_A, 4, "Anterior", fiveSecondsAgo)));

            assertThatThrownBy(() -> service.execute(EVENT_ID, requestWith(null, "Rápido"), USER_A))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("COMMENT_RATE_LIMITED"));

            verify(ratingRepository, never()).save(any());
        }

        @Test
        @DisplayName("comentario a 11 segundos del anterior → se permite")
        void after_window_is_allowed() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            LocalDateTime elevenSecondsAgo = now().minusSeconds(11);
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A))
                    .thenReturn(Optional.of(savedRatingWith(USER_A, 4, "Anterior", elevenSecondsAgo)));
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(true);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, null, "Después", now()));
            stubProfile(USER_A, "Ana");

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, requestWith(null, "Después"), USER_A));
        }

        @Test
        @DisplayName("primer comentario (sin previo) → no hay restricción de tiempo")
        void first_comment_no_restriction() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(Optional.empty());
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, 3, "Bien", now()));
            stubProfile(USER_A, "Ana");

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, requestWith(3, "Bien"), USER_A));
        }
    }

    // ── R1: cualquier rol puede comentar ─────────────────────────────────────

    @Nested
    @DisplayName("R1 — cualquier usuario autenticado puede comentar")
    class AnyRoleCanComment {

        private void stubHappyPath(Long userId) {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, userId)).thenReturn(Optional.empty());
            // Solo USER_A y USER_B no son el organizador (ORGANIZER=1)
            if (!userId.equals(ORGANIZER)) {
                when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, userId)).thenReturn(false);
            }
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(userId, null, "Ok", now()));
            stubNoProfile(userId);
        }

        @Test
        @DisplayName("usuario con rol ORGANIZER (pero no dueño) puede comentar con rating")
        void organizer_role_non_owner_can_comment_with_rating() {
            stubHappyPath(ORGANIZER_USER);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(ORGANIZER_USER, 4, "Bien", now()));

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, requestWith(4, "Bien"), ORGANIZER_USER));
        }

        @Test
        @DisplayName("usuario con rol ADMIN puede comentar")
        void admin_can_comment() {
            stubHappyPath(ADMIN_USER);

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, requestWith(null, "Buen evento"), ADMIN_USER));
        }
    }

    // ── Validaciones básicas ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Validaciones básicas")
    class BasicValidations {

        @Test
        @DisplayName("evento no encontrado → NotFoundException EVENT_NOT_FOUND")
        void event_not_found() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EVENT_ID, requestWith(4, "Ok"), USER_A))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("EVENT_NOT_FOUND"));
            verify(ratingRepository, never()).save(any());
        }

        @Test
        @DisplayName("evento en DRAFT → EVENT_RATING_NOT_ALLOWED")
        void draft_event_rejected() {
            Event draft = Event.builder().eventId(EVENT_ID).userId(ORGANIZER).siteId(1L)
                    .eventName("x").description("x").startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                    .status(EventStatus.DRAFT).isPublic(true).availableSeats(10).build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> service.execute(EVENT_ID, requestWith(4, "ok"), USER_A))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("EVENT_RATING_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("evento privado (isPublic=false) → EVENT_RATING_NOT_ALLOWED")
        void private_event_rejected() {
            Event priv = Event.builder().eventId(EVENT_ID).userId(ORGANIZER).siteId(1L)
                    .eventName("x").description("x").startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                    .status(EventStatus.PUBLISHED).isPublic(false).availableSeats(10).build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(priv));

            assertThatThrownBy(() -> service.execute(EVENT_ID, requestWith(null, "ok"), USER_A))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("EVENT_RATING_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("comentario con solo espacios en blanco → COMMENT_BLANK")
        void blank_comment_rejected() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EVENT_ID, requestWith(null, "   "), USER_A))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("COMMENT_BLANK"));
        }

        @Test
        @DisplayName("evento en estado COMPLETED también permite comentarios")
        void completed_event_allowed() {
            givenClock();
            Event completed = Event.builder().eventId(EVENT_ID).userId(ORGANIZER).siteId(1L)
                    .eventName("x").description("x").startDate(FUTURE.minusDays(10)).finishDate(FUTURE.minusDays(9))
                    .status(EventStatus.COMPLETED).isPublic(true).availableSeats(10).build();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(completed));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(Optional.empty());
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(false);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, 2, "ok", now()));
            stubNoProfile(USER_A);

            assertThatNoException().isThrownBy(() ->
                    service.execute(EVENT_ID, requestWith(2, "ok"), USER_A));
        }
    }

    // ── Listener: evento de dominio publicado correctamente ───────────────────

    @Nested
    @DisplayName("Publicación de EventRatingAddedEvent")
    class DomainEventPublished {

        @Test
        @DisplayName("listener recibe rating null e isOrganizer=false para comentario normal")
        void publishes_null_rating_for_second_comment() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            LocalDateTime prev = now().minusSeconds(30);
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, USER_A))
                    .thenReturn(Optional.of(savedRatingWith(USER_A, 4, "prev", prev)));
            when(ratingRepository.existsRatedCommentByEventIdAndUserId(EVENT_ID, USER_A)).thenReturn(true);
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(USER_A, null, "Otro", now()));
            stubNoProfile(USER_A);

            service.execute(EVENT_ID, requestWith(5, "Otro"), USER_A);

            ArgumentCaptor<EventRatingAddedEvent> captor = ArgumentCaptor.forClass(EventRatingAddedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getRating()).isNull();
            assertThat(captor.getValue().getIsOrganizer()).isFalse();
        }

        @Test
        @DisplayName("listener recibe isOrganizer=true para comentario del organizador")
        void publishes_is_organizer_true_for_owner() {
            givenClock();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedPublicEvent()));
            when(ratingRepository.findLastByEventIdAndUserId(EVENT_ID, ORGANIZER)).thenReturn(Optional.empty());
            when(ratingRepository.save(any())).thenReturn(savedRatingWith(ORGANIZER, null, "Bienvenidos", now()));
            stubNoProfile(ORGANIZER);

            service.execute(EVENT_ID, requestWith(5, "Bienvenidos"), ORGANIZER);

            ArgumentCaptor<EventRatingAddedEvent> captor = ArgumentCaptor.forClass(EventRatingAddedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getIsOrganizer()).isTrue();
            assertThat(captor.getValue().getRating()).isNull();
        }
    }
}
