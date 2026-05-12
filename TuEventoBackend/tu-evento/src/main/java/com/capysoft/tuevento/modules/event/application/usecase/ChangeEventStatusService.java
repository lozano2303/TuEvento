package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;
import com.capysoft.tuevento.modules.event.application.port.in.ChangeEventStatusUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChangeEventStatusService implements ChangeEventStatusUseCase {

    private final EventRepository eventRepository;
    private final EventStatusLogRepository statusLogRepository;
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

        eventRepository.save(Event.builder()
                .eventId(event.getEventId())
                .userId(event.getUserId())
                .siteId(event.getSiteId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .finishDate(event.getFinishDate())
                .status(request.getNewStatus())
                .isPublic(event.isPublic())
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
