package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
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
 * Admin-only status change — identical to ChangeEventStatusService but with
 * the ownership check removed.  The adminUserId is recorded in the audit log.
 */
@Service
@RequiredArgsConstructor
public class AdminChangeEventStatusUseCase implements AdminChangeEventStatusPort {

    private final EventRepository           eventRepository;
    private final EventStatusLogRepository  statusLogRepository;
    private final EventSectionRepository    eventSectionRepository;
    private final EventMediaRepository      eventMediaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EventStatusLogResponse execute(Long eventId, ChangeEventStatusRequest request, Long adminUserId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        // ── No ownership check — admin can act on any event ──────────────────

        validateTransition(event.getStatus(), request.getNewStatus());

        if (request.getNewStatus() == EventStatus.PUBLISHED) {
            // Validar que tenga entre 3 y 9 imágenes (inclusive)
            long mediaCount = eventMediaRepository.countByEventId(eventId);
            if (mediaCount < 3) {
                throw new BusinessException("EVENT_PUBLISH_MEDIA_COUNT_INVALID",
                        "Event must have at least 3 images before publishing (currently has " + mediaCount + ")");
            }
            if (mediaCount > 9) {
                throw new BusinessException("EVENT_PUBLISH_MEDIA_COUNT_INVALID",
                        "Event must have at most 9 images before publishing (currently has " + mediaCount + ")");
            }
            List<EventSection> sections = eventSectionRepository.findAllByEventId(eventId.intValue());
            if (sections.isEmpty()) {
                throw new BusinessException("EVENT_SECTIONS_REQUIRED",
                        "Event cannot be published without at least one section with seats configured");
            }
        }

        if (request.getNewStatus() == EventStatus.COMPLETED) {
            if (!LocalDate.now().isAfter(event.getFinishDate())) {
                throw new BusinessException("EVENT_COMPLETE_TOO_EARLY",
                        "Event can only be completed the day after its finish date");
            }
        }

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
                .build());

        LocalDateTime now = LocalDateTime.now();

        EventStatusLog log = statusLogRepository.save(EventStatusLog.builder()
                .eventId(eventId)
                .oldStatus(event.getStatus())
                .newStatus(request.getNewStatus())
                .changedAt(now)
                .changedBy(adminUserId)   // admin's userId goes into the audit trail
                .build());

        eventPublisher.publishEvent(EventStatusChangedEvent.builder()
                .eventId(eventId)
                .oldStatus(event.getStatus().name())
                .newStatus(request.getNewStatus().name())
                .changedBy(adminUserId)
                .occurredAt(now)
                .build());

        return EventStatusLogResponse.builder()
                .statusLogId(log.getStatusLogId())
                .oldStatus(log.getOldStatus())
                .newStatus(log.getNewStatus())
                .changedAt(log.getChangedAt())
                .changedBy(log.getChangedBy())
                .build();
    }

    private void validateTransition(EventStatus current, EventStatus next) {
        boolean allowed = switch (current) {
            case DRAFT     -> next == EventStatus.PUBLISHED;
            case PUBLISHED -> next == EventStatus.CANCELLED || next == EventStatus.COMPLETED;
            default        -> false;
        };

        if (!allowed) {
            throw new BusinessException("EVENT_INVALID_STATUS_TRANSITION",
                    "Transition from " + current + " to " + next + " is not allowed");
        }
    }
}
