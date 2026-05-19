package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.category.application.dto.request.AssignCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryUseCase;
import com.capysoft.tuevento.modules.event.application.dto.request.CreateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.port.in.CreateEventUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventCreatedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitePort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateEventService implements CreateEventUseCase {

    private final EventRepository           eventRepository;
    private final EventStatusLogRepository  statusLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CategoryUseCase           categoryUseCase;
    private final CategoryEventUseCase      categoryEventUseCase;
    private final GetSitePort               getSitePort;

    @Override
    @Transactional
    public EventResponse execute(CreateEventRequest request, Long userId) {
        if (request.getFinishDate() != null && request.getStartDate() != null
                && !request.getFinishDate().isAfter(request.getStartDate())) {
            throw new BusinessException("EVENT_INVALID_DATES",
                    "finishDate must be after startDate");
        }

        // Validate site exists and seats do not exceed capacity
        SiteResponse site;
        try {
            site = getSitePort.getSite(Math.toIntExact(request.getSiteId()));
        } catch (NotFoundException e) {
            throw new NotFoundException("SITE_NOT_FOUND",
                    "Site not found with id: " + request.getSiteId());
        }
        if (request.getAvailableSeats() > site.getCapacity()) {
            throw new BusinessException("SEATS_EXCEED_CAPACITY",
                    "availableSeats cannot exceed site capacity of " + site.getCapacity());
        }

        if (eventRepository.existsByEventNameAndStartDateAndSiteId(
                request.getEventName(), request.getStartDate(), request.getSiteId())) {
            throw new BusinessException("EVENT_ALREADY_EXISTS",
                    "An event with the same name, start date and site already exists");
        }

        // Validate that the category exists and is active
        CategoryResponse category;
        try {
            category = categoryUseCase.getCategoryById(request.getCategoryId());
        } catch (NotFoundException e) {
            throw new BusinessException("CATEGORY_NOT_FOUND_OR_INACTIVE",
                    "The provided category does not exist or is not active");
        }
        if (!category.isActive()) {
            throw new BusinessException("CATEGORY_NOT_FOUND_OR_INACTIVE",
                    "The provided category does not exist or is not active");
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

        // Assign category — within the same transaction, rolls back on failure
        categoryEventUseCase.assignCategoryToEvent(AssignCategoryRequest.builder()
                .categoryId(request.getCategoryId())
                .eventId(Math.toIntExact(event.getEventId()))
                .build());

        eventPublisher.publishEvent(EventCreatedEvent.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .eventName(event.getEventName())
                .status(EventStatus.DRAFT.name())
                .occurredAt(LocalDateTime.now())
                .build());

        return toResponse(event, request.getCategoryId());
    }

    private EventResponse toResponse(Event e, Integer categoryId) {
        return EventResponse.builder()
                .eventId(e.getEventId())
                .userId(e.getUserId())
                .siteId(e.getSiteId())
                .eventName(e.getEventName())
                .description(e.getDescription())
                .startDate(e.getStartDate())
                .finishDate(e.getFinishDate())
                .status(e.getStatus())
                .isPublic(e.getIsPublic())
                .availableSeats(e.getAvailableSeats())
                .categoryId(categoryId)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }
}
