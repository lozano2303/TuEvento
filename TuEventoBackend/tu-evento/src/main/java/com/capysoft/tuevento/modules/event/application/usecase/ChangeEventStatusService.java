package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.ChangeEventStatusUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
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

@Service
@RequiredArgsConstructor
public class ChangeEventStatusService implements ChangeEventStatusUseCase {

    private final EventRepository          eventRepository;
    private final EventStatusLogRepository statusLogRepository;
    private final EventSectionRepository   eventSectionRepository;
    private final EventMediaRepository     eventMediaRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        if (request.getNewStatus() == EventStatus.PUBLISHED) {
            // 1. Validar que tenga al menos una imagen
            if (eventMediaRepository.countByEventId(eventId) == 0) {
                throw new BusinessException("EVENT_PUBLISH_NO_MEDIA",
                        "Event must have at least one image before publishing");
            }
            // 2. Validar que tenga al menos una sección con sillas configurada
            List<EventSection> sections = eventSectionRepository.findAllByEventId(eventId.intValue());
            if (sections.isEmpty()) {
                throw new BusinessException("EVENT_SECTIONS_REQUIRED",
                        "Event cannot be published without at least one section with seats configured");
            }
        }

        // Validación defensiva: COMPLETED solo puede forzarse manualmente el día después
        // de la fecha de finalización. El caso normal lo cubre el scheduler automático.
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
                .changedBy(userId)
                .build());

        eventPublisher.publishEvent(EventStatusChangedEvent.builder()
                .eventId(eventId)
                .oldStatus(event.getStatus().name())
                .newStatus(request.getNewStatus().name())
                .changedBy(userId)
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
