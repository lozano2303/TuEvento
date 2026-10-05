package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.ChangeEventStatusUseCase;
import com.capysoft.tuevento.modules.event.application.validator.EventDateValidator;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Organizer-facing event status transitions.
 *
 * <h3>Allowed transitions</h3>
 * <pre>
 *   DRAFT          → PENDING_REVIEW  (3 business validations applied)
 *   PENDING_REVIEW → DRAFT           (organizer withdraws submission)
 *   REJECTED       → DRAFT           (organizer corrects and re-submits later)
 *   PUBLISHED      → CANCELLED
 *   PUBLISHED      → COMPLETED       (manual; normally done by the scheduler)
 * </pre>
 *
 * The organizer can no longer publish directly to PUBLISHED — that transition
 * now belongs exclusively to the admin via {@link AdminChangeEventStatusUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ChangeEventStatusService implements ChangeEventStatusUseCase {

    private final EventRepository          eventRepository;
    private final EventStatusLogRepository statusLogRepository;
    private final EventSectionRepository   eventSectionRepository;
    private final EventMediaRepository     eventMediaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EventDateValidator        eventDateValidator;

    @Override
    @Transactional
    public EventStatusLogResponse execute(Long eventId, ChangeEventStatusRequest request, Long userId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (!event.getUserId().equals(userId)) {
            throw new BusinessException("EVENT_ACCESS_DENIED",
                    "User " + userId + " does not own event " + eventId);
        }

        validateTransition(event.getStatus(), request.getNewStatus());

        // ── Business validations per target status ────────────────────────────

        if (request.getNewStatus() == EventStatus.PENDING_REVIEW) {
            // 1. Start date must still be in the future (or today).
            eventDateValidator.validateForPublish(event.getStartDate());

            // 2. Between 3 and 9 images required.
            long mediaCount = eventMediaRepository.countByEventId(eventId);
            if (mediaCount < 3) {
                throw new BusinessException("EVENT_PUBLISH_MEDIA_COUNT_INVALID",
                        "Event must have at least 3 images before submitting for review (currently has "
                                + mediaCount + ")");
            }
            if (mediaCount > 9) {
                throw new BusinessException("EVENT_PUBLISH_MEDIA_COUNT_INVALID",
                        "Event must have at most 9 images before submitting for review (currently has "
                                + mediaCount + ")");
            }

            // 3. At least one section with seats configured.
            List<EventSection> sections = eventSectionRepository.findAllByEventId(eventId.intValue());
            if (sections.isEmpty()) {
                throw new BusinessException("EVENT_SECTIONS_REQUIRED",
                        "Event cannot be submitted for review without at least one section with seats configured");
            }
        }

        // COMPLETED can only be set the day after the finish date.
        if (request.getNewStatus() == EventStatus.COMPLETED) {
            if (!LocalDate.now().isAfter(event.getFinishDate())) {
                throw new BusinessException("EVENT_COMPLETE_TOO_EARLY",
                        "Event can only be completed the day after its finish date");
            }
        }

        // ── Persist updated event ─────────────────────────────────────────────

        // When reverting to DRAFT (from PENDING_REVIEW or REJECTED), clear rejectionReason.
        String newRejectionReason = (request.getNewStatus() == EventStatus.DRAFT)
                ? null
                : event.getRejectionReason();

        eventRepository.save(Event.builder()
                .eventId(event.getEventId())
                .userId(event.getUserId())
                .siteId(event.getSiteId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .finishDate(event.getFinishDate())
                .status(request.getNewStatus())
                .isPublic(event.getIsPublic())
                .availableSeats(event.getAvailableSeats())
                .rejectionReason(newRejectionReason)
                .build());

        // ── Audit log ─────────────────────────────────────────────────────────

        LocalDateTime now = LocalDateTime.now();

        EventStatusLog log = statusLogRepository.save(EventStatusLog.builder()
                .eventId(eventId)
                .oldStatus(event.getStatus())
                .newStatus(request.getNewStatus())
                .changedAt(now)
                .changedBy(userId)
                .build());

        // ── Domain event (consumed by Phase 4 notification listener) ──────────

        eventPublisher.publishEvent(EventStatusChangedEvent.builder()
                .eventId(eventId)
                .oldStatus(event.getStatus().name())
                .newStatus(request.getNewStatus().name())
                .changedBy(userId)
                .occurredAt(now)
                .organizerId(event.getUserId())
                .eventName(event.getEventName())
                .reason(null) // organizer transitions never carry a rejection reason
                .build());

        return EventStatusLogResponse.builder()
                .statusLogId(log.getStatusLogId())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .changedAt(log.getChangedAt())
                .changedBy(log.getChangedBy())
                .build();
    }

    // ── Transition table (organizer) ──────────────────────────────────────────

    private void validateTransition(EventStatus current, EventStatus next) {
        boolean allowed = switch (current) {
            case DRAFT          -> next == EventStatus.PENDING_REVIEW;
            case PENDING_REVIEW -> next == EventStatus.DRAFT;
            case REJECTED       -> next == EventStatus.DRAFT;
            case PUBLISHED      -> next == EventStatus.CANCELLED || next == EventStatus.COMPLETED;
            default             -> false;
        };

        if (!allowed) {
            throw new BusinessException("EVENT_INVALID_STATUS_TRANSITION",
                    "Transition from " + current + " to " + next + " is not allowed");
        }
    }
}
