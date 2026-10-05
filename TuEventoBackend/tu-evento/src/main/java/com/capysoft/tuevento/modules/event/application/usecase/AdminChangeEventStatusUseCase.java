package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
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
 * Admin-only event status transitions — bypasses ownership enforcement so
 * an admin can act on any event regardless of who created it.
 * The admin's userId is recorded in the audit log.
 *
 * <h3>Allowed transitions</h3>
 * <pre>
 *   PENDING_REVIEW → PUBLISHED    (admin approves; 3 business validations re-applied)
 *   PENDING_REVIEW → REJECTED     (admin rejects; reason is required and non-blank)
 *   PUBLISHED      → CANCELLED
 *   PUBLISHED      → COMPLETED    (manual; normally done by the scheduler)
 * </pre>
 *
 * The admin can no longer publish from DRAFT directly; the event must first
 * be submitted for review by the organizer.
 */
@Service
@RequiredArgsConstructor
public class AdminChangeEventStatusUseCase implements AdminChangeEventStatusPort {

    private final EventRepository           eventRepository;
    private final EventStatusLogRepository  statusLogRepository;
    private final EventSectionRepository    eventSectionRepository;
    private final EventMediaRepository      eventMediaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EventDateValidator        eventDateValidator;

    @Override
    @Transactional
    public EventStatusLogResponse execute(Long eventId, ChangeEventStatusRequest request, Long adminUserId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        // No ownership check — admin can act on any event.

        validateTransition(event.getStatus(), request.getNewStatus());

        // ── Business validations per target status ────────────────────────────

        if (request.getNewStatus() == EventStatus.PUBLISHED) {
            // Re-validate start date: the event may have been sitting in PENDING_REVIEW
            // for a while and its start date could now be in the past.
            eventDateValidator.validateForPublish(event.getStartDate());

            // Between 3 and 9 images required.
            long mediaCount = eventMediaRepository.countByEventId(eventId);
            if (mediaCount < 3) {
                throw new BusinessException("EVENT_PUBLISH_MEDIA_COUNT_INVALID",
                        "Event must have at least 3 images before publishing (currently has "
                                + mediaCount + ")");
            }
            if (mediaCount > 9) {
                throw new BusinessException("EVENT_PUBLISH_MEDIA_COUNT_INVALID",
                        "Event must have at most 9 images before publishing (currently has "
                                + mediaCount + ")");
            }

            // At least one section with seats.
            List<EventSection> sections = eventSectionRepository.findAllByEventId(eventId.intValue());
            if (sections.isEmpty()) {
                throw new BusinessException("EVENT_SECTIONS_REQUIRED",
                        "Event cannot be published without at least one section with seats configured");
            }
        }

        if (request.getNewStatus() == EventStatus.REJECTED) {
            // Rejection reason is mandatory and must not be blank.
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new BusinessException("EVENT_REJECTION_REASON_REQUIRED",
                        "A non-blank rejection reason is required when rejecting an event");
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

        // Store rejection reason on the event when rejecting; clear it when publishing.
        String newRejectionReason = switch (request.getNewStatus()) {
            case REJECTED  -> request.getReason();
            case PUBLISHED -> null;
            default        -> event.getRejectionReason();
        };

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
                .changedBy(adminUserId)   // admin's userId goes into the audit trail
                .reason(request.getReason())
                .build());

        // ── Domain event (consumed by Phase 4 notification listener) ──────────

        eventPublisher.publishEvent(EventStatusChangedEvent.builder()
                .eventId(eventId)
                .oldStatus(event.getStatus().name())
                .newStatus(request.getNewStatus().name())
                .changedBy(adminUserId)
                .occurredAt(now)
                .organizerId(event.getUserId())
                .eventName(event.getEventName())
                .reason(request.getNewStatus() == EventStatus.REJECTED ? request.getReason() : null)
                .build());

        return EventStatusLogResponse.builder()
                .statusLogId(log.getStatusLogId())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .changedAt(log.getChangedAt())
                .changedBy(log.getChangedBy())
                .reason(log.getReason())
                .build();
    }

    // ── Transition table (admin) ──────────────────────────────────────────────

    private void validateTransition(EventStatus current, EventStatus next) {
        boolean allowed = switch (current) {
            case PENDING_REVIEW -> next == EventStatus.PUBLISHED || next == EventStatus.REJECTED;
            case PUBLISHED      -> next == EventStatus.CANCELLED || next == EventStatus.COMPLETED;
            default             -> false;
        };

        if (!allowed) {
            throw new BusinessException("EVENT_INVALID_STATUS_TRANSITION",
                    "Transition from " + current + " to " + next + " is not allowed");
        }
    }
}
