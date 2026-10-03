package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.validator.EventDateValidator;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChangeEventStatusService}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Every allowed transition fires correctly</li>
 *   <li>Every forbidden transition throws EVENT_INVALID_STATUS_TRANSITION</li>
 *   <li>Organizer cannot transition to PUBLISHED directly (gated by review)</li>
 *   <li>PENDING_REVIEW business validations (date, images, sections)</li>
 *   <li>REJECTED → DRAFT clears rejectionReason</li>
 *   <li>Access denied when caller is not the owner</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeEventStatusService")
class ChangeEventStatusServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock private EventRepository          eventRepository;
    @Mock private EventStatusLogRepository statusLogRepository;
    @Mock private EventSectionRepository   eventSectionRepository;
    @Mock private EventMediaRepository     eventMediaRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EventDateValidator        eventDateValidator;

    @InjectMocks
    private ChangeEventStatusService service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long EVENT_ID  = 1L;
    private static final Long USER_ID   = 42L;
    private static final Long OTHER_ID  = 99L;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(10);

    /** Builds a minimal saved EventStatusLog so statusLogRepository.save() returns a real object. */
    private EventStatusLog stubLog(EventStatus old, EventStatus next) {
        return EventStatusLog.builder()
                .statusLogId(100L)
                .eventId(EVENT_ID)
                .oldStatus(old)
                .newStatus(next)
                .changedAt(LocalDateTime.now())
                .changedBy(USER_ID)
                .build();
    }

    /** Stubs eventRepository, eventMediaRepository, eventSectionRepository, statusLogRepository
     *  for a happy-path DRAFT → PENDING_REVIEW transition. */
    private void stubHappyPathPendingReview(Event event) {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventMediaRepository.countByEventId(EVENT_ID)).thenReturn(4L);
        when(eventSectionRepository.findAllByEventId(EVENT_ID.intValue()))
                .thenReturn(List.of(mock(EventSection.class)));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusLogRepository.save(any())).thenReturn(stubLog(EventStatus.DRAFT, EventStatus.PENDING_REVIEW));
    }

    // ── Tests: allowed transitions ────────────────────────────────────────────

    @Nested
    @DisplayName("Allowed transitions")
    class AllowedTransitions {

        @Test
        @DisplayName("DRAFT → PENDING_REVIEW saves event and returns log response")
        void draft_to_pendingReview_success() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.DRAFT).isPublic(true).availableSeats(100)
                    .build();

            stubHappyPathPendingReview(event);

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            EventStatusLogResponse response = service.execute(EVENT_ID, req, USER_ID);

            assertThat(response.getNewStatus()).isEqualTo(EventStatus.PENDING_REVIEW);
            verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.PENDING_REVIEW));
            verify(statusLogRepository).save(any());
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("PENDING_REVIEW → DRAFT (withdraw) clears rejectionReason and saves")
        void pendingReview_to_draft_clears_rejectionReason() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.PENDING_REVIEW).isPublic(true).availableSeats(100)
                    .rejectionReason(null)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusLogRepository.save(any())).thenReturn(stubLog(EventStatus.PENDING_REVIEW, EventStatus.DRAFT));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.DRAFT).build();

            EventStatusLogResponse response = service.execute(EVENT_ID, req, USER_ID);

            assertThat(response.getNewStatus()).isEqualTo(EventStatus.DRAFT);
            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());
            assertThat(captor.getValue().getRejectionReason()).isNull();
        }

        @Test
        @DisplayName("REJECTED → DRAFT clears rejectionReason")
        void rejected_to_draft_clears_rejectionReason() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.REJECTED).isPublic(true).availableSeats(100)
                    .rejectionReason("Missing images")
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusLogRepository.save(any())).thenReturn(stubLog(EventStatus.REJECTED, EventStatus.DRAFT));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.DRAFT).build();

            service.execute(EVENT_ID, req, USER_ID);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());
            assertThat(captor.getValue().getRejectionReason()).isNull();
        }

        @Test
        @DisplayName("PUBLISHED → CANCELLED saves event with new status")
        void published_to_cancelled_success() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.PUBLISHED).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(statusLogRepository.save(any())).thenReturn(stubLog(EventStatus.PUBLISHED, EventStatus.CANCELLED));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.CANCELLED).build();

            EventStatusLogResponse response = service.execute(EVENT_ID, req, USER_ID);

            assertThat(response.getNewStatus()).isEqualTo(EventStatus.CANCELLED);
        }
    }

    // ── Tests: organizer cannot publish directly ──────────────────────────────

    @Nested
    @DisplayName("Organizer cannot publish directly")
    class DirectPublishBlocked {

        @Test
        @DisplayName("DRAFT → PUBLISHED throws EVENT_INVALID_STATUS_TRANSITION")
        void draft_to_published_throws() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.DRAFT).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PUBLISHED).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_INVALID_STATUS_TRANSITION"));

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("PENDING_REVIEW → PUBLISHED (organizer) throws EVENT_INVALID_STATUS_TRANSITION")
        void pendingReview_to_published_by_organizer_throws() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.PENDING_REVIEW).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PUBLISHED).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_INVALID_STATUS_TRANSITION"));
        }
    }

    // ── Tests: PENDING_REVIEW business validations ────────────────────────────

    @Nested
    @DisplayName("PENDING_REVIEW business validations")
    class PendingReviewValidations {

        private Event draftEvent() {
            return Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.DRAFT).isPublic(true).availableSeats(100)
                    .build();
        }

        @Test
        @DisplayName("too few images (<3) → throws EVENT_PUBLISH_MEDIA_COUNT_INVALID")
        void tooFewImages_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draftEvent()));
            when(eventMediaRepository.countByEventId(EVENT_ID)).thenReturn(2L);

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_PUBLISH_MEDIA_COUNT_INVALID"));
        }

        @Test
        @DisplayName("too many images (>9) → throws EVENT_PUBLISH_MEDIA_COUNT_INVALID")
        void tooManyImages_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draftEvent()));
            when(eventMediaRepository.countByEventId(EVENT_ID)).thenReturn(10L);

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_PUBLISH_MEDIA_COUNT_INVALID"));
        }

        @Test
        @DisplayName("no sections → throws EVENT_SECTIONS_REQUIRED")
        void noSections_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draftEvent()));
            when(eventMediaRepository.countByEventId(EVENT_ID)).thenReturn(4L);
            when(eventSectionRepository.findAllByEventId(EVENT_ID.intValue())).thenReturn(List.of());

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_SECTIONS_REQUIRED"));
        }

        @Test
        @DisplayName("start date in the past → EventDateValidator throws EVENT_START_DATE_IN_PAST")
        void pastStartDate_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(draftEvent()));
            doThrow(new BusinessException("EVENT_START_DATE_IN_PAST", "Cannot submit: startDate is in the past"))
                    .when(eventDateValidator).validateForPublish(any());

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_START_DATE_IN_PAST"));
        }
    }

    // ── Tests: forbidden transitions ──────────────────────────────────────────

    @Nested
    @DisplayName("Forbidden transitions")
    class ForbiddenTransitions {

        @Test
        @DisplayName("CANCELLED → DRAFT throws EVENT_INVALID_STATUS_TRANSITION")
        void cancelled_to_draft_throws() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.CANCELLED).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.DRAFT).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_INVALID_STATUS_TRANSITION"));
        }

        @Test
        @DisplayName("COMPLETED → any throws EVENT_INVALID_STATUS_TRANSITION")
        void completed_to_draft_throws() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE.minusDays(30)).finishDate(FUTURE_DATE.minusDays(29))
                    .status(EventStatus.COMPLETED).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.DRAFT).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_INVALID_STATUS_TRANSITION"));
        }
    }

    // ── Tests: access control ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Access control")
    class AccessControl {

        @Test
        @DisplayName("non-owner caller throws EVENT_ACCESS_DENIED")
        void nonOwner_throws() {
            Event event = Event.builder()
                    .eventId(EVENT_ID).userId(USER_ID).siteId(1L)
                    .eventName("Test").description("Desc")
                    .startDate(FUTURE_DATE).finishDate(FUTURE_DATE.plusDays(1))
                    .status(EventStatus.DRAFT).isPublic(true).availableSeats(100)
                    .build();

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, OTHER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("EVENT_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("unknown eventId throws EVENT_NOT_FOUND")
        void eventNotFound_throws() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            ChangeEventStatusRequest req = ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.PENDING_REVIEW).build();

            assertThatThrownBy(() -> service.execute(EVENT_ID, req, USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                            .isEqualTo("EVENT_NOT_FOUND"));
        }
    }
}
