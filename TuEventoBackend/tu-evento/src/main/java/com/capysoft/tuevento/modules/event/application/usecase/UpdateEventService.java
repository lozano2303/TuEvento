package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.UpdateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.port.in.UpdateEventUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UpdateEventService implements UpdateEventUseCase {

    private final EventRepository eventRepository;

    @Override
    @Transactional
    public EventResponse execute(Long eventId, UpdateEventRequest request, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (!event.getUserId().equals(userId)) {
            throw new BusinessException("EVENT_ACCESS_DENIED",
                    "User " + userId + " does not own event " + eventId);
        }

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("EVENT_UPDATE_NOT_ALLOWED",
                    "Only DRAFT events can be updated");
        }

        String eventName      = request.getEventName()      != null ? request.getEventName()      : event.getEventName();
        String description    = request.getDescription()    != null ? request.getDescription()    : event.getDescription();
        Long siteId           = request.getSiteId()         != null ? request.getSiteId()         : event.getSiteId();
        LocalDate startDate   = request.getStartDate()      != null ? request.getStartDate()      : event.getStartDate();
        LocalDate finishDate  = request.getFinishDate()     != null ? request.getFinishDate()     : event.getFinishDate();
        Boolean isPublic      = request.getIsPublic()       != null ? request.getIsPublic()       : event.getIsPublic();
        int availableSeats    = request.getAvailableSeats() != null ? request.getAvailableSeats() : event.getAvailableSeats();

        if (!finishDate.isAfter(startDate)) {
            throw new BusinessException("EVENT_INVALID_DATES",
                    "finishDate must be after startDate");
        }

        Event updated = eventRepository.save(Event.builder()
                .eventId(event.getEventId())
                .userId(event.getUserId())
                .siteId(siteId)
                .eventName(eventName)
                .description(description)
                .startDate(startDate)
                .finishDate(finishDate)
                .status(event.getStatus())
                .isPublic(isPublic)
                .availableSeats(availableSeats)
                .build());

        return EventResponse.builder()
                .eventId(updated.getEventId())
                .userId(updated.getUserId())
                .siteId(updated.getSiteId())
                .eventName(updated.getEventName())
                .description(updated.getDescription())
                .startDate(updated.getStartDate())
                .finishDate(updated.getFinishDate())
                .status(updated.getStatus())
                .isPublic(updated.getIsPublic())
                .availableSeats(updated.getAvailableSeats())
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .createdBy(updated.getCreatedBy())
                .updatedBy(updated.getUpdatedBy())
                .build();
    }
}
