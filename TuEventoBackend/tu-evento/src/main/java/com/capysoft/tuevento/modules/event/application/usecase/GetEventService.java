package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetEventUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetEventService implements GetEventUseCase {

    private final EventRepository eventRepository;

    @Override
    public EventResponse findById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));
        return toResponse(event);
    }

    @Override
    public List<EventSummaryResponse> findByUser(Long userId) {
        return eventRepository.findByUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> findByStatus(EventStatus status) {
        return eventRepository.findByStatus(status).stream()
                .map(this::toSummary)
                .toList();
    }

    private EventResponse toResponse(Event e) {
        return EventResponse.builder()
                .eventId(e.getEventId())
                .userId(e.getUserId())
                .siteId(e.getSiteId())
                .siteName(null) // resolved by caller when needed
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

    private EventSummaryResponse toSummary(Event e) {
        return EventSummaryResponse.builder()
                .eventId(e.getEventId())
                .eventName(e.getEventName())
                .status(e.getStatus())
                .startDate(e.getStartDate())
                .finishDate(e.getFinishDate())
                .isPublic(e.isPublic())
                .availableSeats(e.getAvailableSeats())
                .build();
    }
}
