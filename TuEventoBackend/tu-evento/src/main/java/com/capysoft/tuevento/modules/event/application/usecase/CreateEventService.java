package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.CreateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.port.in.CreateEventUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventCreatedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateEventService implements CreateEventUseCase {

    private final EventRepository eventRepository;
    private final EventStatusLogRepository statusLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EventResponse execute(CreateEventRequest request, Long userId) {
        if (request.getFinishDate() != null && request.getStartDate() != null
                && !request.getFinishDate().isAfter(request.getStartDate())) {
            throw new BusinessException("EVENT_INVALID_DATES",
                    "finishDate must be after startDate");
        }

        if (eventRepository.existsByEventNameAndStartDateAndSiteId(
                request.getEventName(), request.getStartDate(), request.getSiteId())) {
            throw new BusinessException("EVENT_ALREADY_EXISTS",
                    "An event with the same name, start date and site already exists");
        }

        Event event = eventRepository.save(Event.builder()
                .userId(userId)
                .siteId(request.getSiteId())
                .eventName(request.getEventName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .finishDate(request.getFinishDate())
                .status(EventStatus.DRAFT)
                .isPublic(request.isPublic())
                .availableSeats(request.getAvailableSeats())
                .build());

        statusLogRepository.save(EventStatusLog.builder()
                .eventId(event.getEventId())
                .oldStatus(null)
                .newStatus(EventStatus.DRAFT)
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .build());

        eventPublisher.publishEvent(EventCreatedEvent.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .eventName(event.getEventName())
                .status(EventStatus.DRAFT.name())
                .occurredAt(LocalDateTime.now())
                .build());

        return toResponse(event);
    }

    private EventResponse toResponse(Event e) {
        return EventResponse.builder()
                .eventId(e.getEventId())
                .userId(e.getUserId())
                .siteId(e.getSiteId())
                .eventName(e.getEventName())
                .description(e.getDescription())
                .startDate(e.getStartDate())
                .finishDate(e.getFinishDate())
                .status(e.getStatus())
                .isPublic(e.isPublic())
                .availableSeats(e.getAvailableSeats())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }
}
