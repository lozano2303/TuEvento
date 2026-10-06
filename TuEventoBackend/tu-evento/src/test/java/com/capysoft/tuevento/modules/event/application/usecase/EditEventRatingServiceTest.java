package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EditEventRatingService")
class EditEventRatingServiceTest {

    @Mock private EventRatingRepository     ratingRepository;
    @Mock private EventRepository           eventRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProfileJpaRepository      profileJpaRepository;
    @Mock private Clock                     clock;

    @InjectMocks private EditEventRatingService service;

    private static final Long   EVENT_ID   = 10L;
    private static final Long   RATING_ID  = 42L;
    private static final Long   USER_ID    = 7L;
    private static final Long   ORGANIZER  = 1L;   // dueño del evento
    private static final Long   OTHER_USER = 8L;

    private static final ZoneId    BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate FUTURE = LocalDate.now().plusDays(5);

    private void givenClock(Instant instant) {
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(BOGOTA);
    }

    private LocalDateTime toLocal(Instant i) {
        return LocalDateTime.ofInstant(i, BOGOTA);
    }

    private Event publishedEvent(Long ownerId) {
        return Event.builder()
                .eventId(EVENT_ID).userId(ownerId).siteId(1L)
                .eventName("Rock Fest").description("Desc")
                .startDate(FUTURE).finishDate(FUTURE.plusDays(1))
                .status(EventStatus.PUBLISHED).isPublic(true).availableSeats(200)
                .build();
    }

    private EventRating existingRating(Long userId, LocalDateTime createdAt) {
        return EventRating.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(userId)
                .rating(4).comment("texto original").isVisible(true)
                .createdAt(createdAt)
                .build();
    }

    private EventRating savedWith(Long userId, String comment, LocalDateTime createdAt, LocalDateTime editedAt) {
        return EventRating.builder()
                .ratingId(RATING_ID).eventId(EVENT_ID).userId(userId)
                .rating(4).comment(comment).isVisible(true)
                .createdAt(createdAt).editedAt(editedAt)
                .build();
    }

    private void stubProfile(Long userId, String name) {
        ProfileEntity p = new ProfileEntity();
        p.setFullName(name);
        p.setUserId(userId.intValue());
        when(profileJpaRepository.findByUserId(userId.intValue())).thenReturn(Optional.of(p));
    }

    @Test @DisplayName("autor edita dentro de 2 h → OK con editedAt poblado")
    void author_edits_within_window() {
        Instant now = Instant.parse("2026-10-05T14:30:00Z");
        Instant createdInstant = now.minusSeconds(3600);
        givenClock(now);
        LocalDateTime createdAt = toLocal(createdInstant);
        LocalDateTime editedAt  = toLocal(now);

        when(ratingRepository.findById(RATING_ID))
                .thenReturn(Optional.of(existingRating(USER_ID, createdAt)));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(publishedEvent(ORGANIZER)));
        when(ratingRepository.save(any()))
                .thenReturn(savedWith(USER_ID, "texto editado", createdAt, editedAt));
        when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.empty());

        EventRatingResponse res = service.execute(EVENT_ID, RATING_ID, USER_ID, "texto editado");

        assertThat(res.getComment()).isEqualTo("texto editado");
        assertThat(res.getEditedAt()).isNotNull();
        assertThat(res.getIsOrganizer()).isFalse();
    }

    @Test @DisplayName("exactamente 2 h después → COMMENT_EDIT_WINDOW_EXPIRED (Clock controlable)")
    void author_edits_exactly_at_window_boundary_rejected() {
        Instant now = Instant.parse("2026-10-05T14:30:00Z");
        Instant createdInstant = now.minusSeconds(2 * 3600L);
        givenClock(now);
        LocalDateTime createdAt = toLocal(createdInstant);

        when(ratingRepository.findById(RATING_ID))
                .thenReturn(Optional.of(existingRating(USER_ID, createdAt)));

        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, USER_ID, "nuevo texto"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("COMMENT_EDIT_WINDOW_EXPIRED"));
        verify(ratingRepository, never()).save(any());
    }

    @Test @DisplayName("otro usuario intenta editar → RATING_ACCESS_DENIED")
    void non_author_cannot_edit() {
        Instant now = Instant.parse("2026-10-05T14:30:00Z");
        when(ratingRepository.findById(RATING_ID))
                .thenReturn(Optional.of(existingRating(USER_ID, toLocal(now.minusSeconds(60)))));

        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, OTHER_USER, "intento"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("RATING_ACCESS_DENIED"));
        verify(ratingRepository, never()).save(any());
    }

    @Test @DisplayName("comentario inexistente → NotFoundException RATING_NOT_FOUND")
    void rating_not_found() {
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, USER_ID, "texto"))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                        .isEqualTo("RATING_NOT_FOUND"));
    }

    @Test @DisplayName("texto vacío → COMMENT_BLANK")
    void blank_text_rejected() {
        Instant now = Instant.parse("2026-10-05T14:30:00Z");
        givenClock(now);
        when(ratingRepository.findById(RATING_ID))
                .thenReturn(Optional.of(existingRating(USER_ID, toLocal(now.minusSeconds(60)))));

        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, USER_ID, "   "))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("COMMENT_BLANK"));
    }

    @Test @DisplayName("texto de más de 500 caracteres → COMMENT_TOO_LONG")
    void too_long_text_rejected() {
        Instant now = Instant.parse("2026-10-05T14:30:00Z");
        givenClock(now);
        when(ratingRepository.findById(RATING_ID))
                .thenReturn(Optional.of(existingRating(USER_ID, toLocal(now.minusSeconds(60)))));

        assertThatThrownBy(() -> service.execute(EVENT_ID, RATING_ID, USER_ID, "a".repeat(501)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("COMMENT_TOO_LONG"));
    }

    @Nested @DisplayName("isOrganizer en el evento publicado")
    class IsOrganizerInEvent {

        @Test @DisplayName("el organizador del evento edita → payload lleva isOrganizer = true")
        void organizer_edits_carries_is_organizer_true() {
            Instant now = Instant.parse("2026-10-05T14:30:00Z");
            givenClock(now);
            LocalDateTime createdAt = toLocal(now.minusSeconds(60));
            LocalDateTime editedAt  = toLocal(now);

            // ORGANIZER es el dueño del evento
            when(ratingRepository.findById(RATING_ID))
                    .thenReturn(Optional.of(existingRating(ORGANIZER, createdAt)));
            when(eventRepository.findById(EVENT_ID))
                    .thenReturn(Optional.of(publishedEvent(ORGANIZER)));
            when(ratingRepository.save(any()))
                    .thenReturn(savedWith(ORGANIZER, "texto del org", createdAt, editedAt));
            stubProfile(ORGANIZER, "Carlos Org");

            EventRatingResponse res =
                    service.execute(EVENT_ID, RATING_ID, ORGANIZER, "texto del org");

            assertThat(res.getIsOrganizer()).isTrue();

            ArgumentCaptor<EventRatingUpdatedEvent> captor =
                    ArgumentCaptor.forClass(EventRatingUpdatedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getIsOrganizer()).isTrue();
        }

        @Test @DisplayName("usuario normal edita → payload lleva isOrganizer = false")
        void normal_user_edits_carries_is_organizer_false() {
            Instant now = Instant.parse("2026-10-05T14:30:00Z");
            givenClock(now);
            LocalDateTime createdAt = toLocal(now.minusSeconds(60));
            LocalDateTime editedAt  = toLocal(now);

            when(ratingRepository.findById(RATING_ID))
                    .thenReturn(Optional.of(existingRating(USER_ID, createdAt)));
            when(eventRepository.findById(EVENT_ID))
                    .thenReturn(Optional.of(publishedEvent(ORGANIZER)));  // ORGANIZER != USER_ID
            when(ratingRepository.save(any()))
                    .thenReturn(savedWith(USER_ID, "editado normal", createdAt, editedAt));
            when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.empty());

            EventRatingResponse res =
                    service.execute(EVENT_ID, RATING_ID, USER_ID, "editado normal");

            assertThat(res.getIsOrganizer()).isFalse();

            ArgumentCaptor<EventRatingUpdatedEvent> captor =
                    ArgumentCaptor.forClass(EventRatingUpdatedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getIsOrganizer()).isFalse();
        }
    }

    @Nested @DisplayName("Evento de dominio y antispam")
    class DomainEventAndAntispam {

        @Test @DisplayName("el listener recibe payload completo (misma forma que GET /ratings)")
        void publishes_full_payload() {
            Instant now = Instant.parse("2026-10-05T14:30:00Z");
            givenClock(now);
            LocalDateTime createdAt = toLocal(now.minusSeconds(60));
            LocalDateTime editedAt  = toLocal(now);

            when(ratingRepository.findById(RATING_ID))
                    .thenReturn(Optional.of(existingRating(USER_ID, createdAt)));
            when(eventRepository.findById(EVENT_ID))
                    .thenReturn(Optional.of(publishedEvent(ORGANIZER)));
            when(ratingRepository.save(any()))
                    .thenReturn(savedWith(USER_ID, "editado", createdAt, editedAt));
            when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.empty());

            service.execute(EVENT_ID, RATING_ID, USER_ID, "editado");

            ArgumentCaptor<EventRatingUpdatedEvent> captor =
                    ArgumentCaptor.forClass(EventRatingUpdatedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            EventRatingUpdatedEvent evt = captor.getValue();
            assertThat(evt.getEditedAt()).isEqualTo(editedAt);
            assertThat(evt.getRatingId()).isEqualTo(RATING_ID);
            assertThat(evt.getIsOrganizer()).isFalse();
            assertThat(evt.getAuthorName()).isEqualTo("Usuario");  // fallback sin perfil
            assertThat(evt.getRating()).isEqualTo(4);
            assertThat(evt.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test @DisplayName("editar no invoca findLastByEventIdAndUserId (no activa el antispam)")
        void edit_does_not_trigger_antispam() {
            Instant now = Instant.parse("2026-10-05T14:30:00Z");
            givenClock(now);
            LocalDateTime createdAt = toLocal(now.minusSeconds(60));
            LocalDateTime editedAt  = toLocal(now);

            when(ratingRepository.findById(RATING_ID))
                    .thenReturn(Optional.of(existingRating(USER_ID, createdAt)));
            when(eventRepository.findById(EVENT_ID))
                    .thenReturn(Optional.of(publishedEvent(ORGANIZER)));
            when(ratingRepository.save(any()))
                    .thenReturn(savedWith(USER_ID, "editado", createdAt, editedAt));
            when(profileJpaRepository.findByUserId(USER_ID.intValue())).thenReturn(Optional.empty());

            service.execute(EVENT_ID, RATING_ID, USER_ID, "editado");

            verify(ratingRepository, never()).findLastByEventIdAndUserId(anyLong(), anyLong());
        }
    }
}
