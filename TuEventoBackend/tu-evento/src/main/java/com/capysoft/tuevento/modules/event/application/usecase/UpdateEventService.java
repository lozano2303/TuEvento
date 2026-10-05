package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.category.application.dto.request.AssignCategoryRequest;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryUseCase;
import com.capysoft.tuevento.modules.event.application.dto.request.UpdateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.port.in.UpdateEventUseCase;
import com.capysoft.tuevento.modules.event.application.validator.EventDateValidator;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitePort;
import com.capysoft.tuevento.modules.language.application.service.TranslationService;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateEventService implements UpdateEventUseCase {

    private final EventRepository      eventRepository;
    private final GetSitePort          getSitePort;
    private final CategoryUseCase      categoryUseCase;
    private final CategoryEventUseCase categoryEventUseCase;
    private final EventDateValidator   eventDateValidator;
    private final TranslationService   translationService;

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

        String    eventName      = request.getEventName()      != null ? request.getEventName()      : event.getEventName();
        String    description    = request.getDescription()    != null ? request.getDescription()    : event.getDescription();
        Long      siteId         = request.getSiteId()         != null ? request.getSiteId()         : event.getSiteId();
        LocalDate startDate      = request.getStartDate()      != null ? request.getStartDate()      : event.getStartDate();
        LocalDate finishDate     = request.getFinishDate()     != null ? request.getFinishDate()     : event.getFinishDate();
        Boolean   isPublic       = request.getIsPublic()       != null ? request.getIsPublic()       : event.getIsPublic();
        int       availableSeats = request.getAvailableSeats() != null ? request.getAvailableSeats() : event.getAvailableSeats();

        // Validate dates only when the caller explicitly changed at least one of them.
        // This lets organizers edit other fields (name, description, etc.) on events
        // whose dates are already in the past without being blocked.
        boolean startDateChanged  = request.getStartDate()  != null && !request.getStartDate().equals(event.getStartDate());
        boolean finishDateChanged = request.getFinishDate() != null && !request.getFinishDate().equals(event.getFinishDate());
        if (startDateChanged || finishDateChanged) {
            eventDateValidator.validate(startDate, finishDate);
        }

        // Validate site exists and seats do not exceed capacity
        SiteResponse site;
        try {
            site = getSitePort.getSite(Math.toIntExact(siteId));
        } catch (NotFoundException e) {
            throw new NotFoundException("SITE_NOT_FOUND",
                    "Site not found with id: " + siteId);
        }
        if (availableSeats > site.getCapacity()) {
            throw new BusinessException("SEATS_EXCEED_CAPACITY",
                    "availableSeats cannot exceed site capacity of " + site.getCapacity());
        }

        // FIX 1: Update category if provided
        if (request.getCategoryId() != null) {
            CategoryResponse newCategory;
            try {
                newCategory = categoryUseCase.getCategoryById(request.getCategoryId());
            } catch (NotFoundException e) {
                throw new NotFoundException("CATEGORY_NOT_FOUND_OR_INACTIVE",
                        "The provided category does not exist or is not active");
            }
            if (!newCategory.isActive()) {
                throw new BusinessException("CATEGORY_NOT_FOUND_OR_INACTIVE",
                        "The provided category does not exist or is not active");
            }
            // Remove current category assignment and assign the new one
            categoryEventUseCase.removeAllCategoriesFromEvent(Math.toIntExact(event.getEventId()));
            categoryEventUseCase.assignCategoryToEvent(AssignCategoryRequest.builder()
                    .categoryId(request.getCategoryId())
                    .eventId(Math.toIntExact(event.getEventId()))
                    .build());
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
                .rejectionReason(event.getRejectionReason())
                .build());

        // Request translation if content changed (name or description)
        boolean contentChanged = !eventName.equals(event.getEventName()) || 
                                !description.equals(event.getDescription());
        
        if (contentChanged) {
            try {
                translationService.requestTranslation("event", updated.getEventId(), "es");
                log.debug("Translation requested for updated event {}", updated.getEventId());
            } catch (Exception e) {
                log.warn("Failed to request translation for event {}: {}", updated.getEventId(), e.getMessage());
                // Non-blocking: continue even if translation fails
            }
        }

        // FIX 2: Resolve categoryId for response — fail-soft
        Integer resolvedCategoryId = null;
        try {
            List<CategoryResponse> cats = categoryEventUseCase.getCategoriesByEvent(
                    Math.toIntExact(updated.getEventId()));
            resolvedCategoryId = cats.isEmpty() ? null : cats.get(0).getCategoryId();
        } catch (Exception ex) {
            log.warn("Could not resolve categoryId for event {}: {}", updated.getEventId(), ex.getMessage());
        }

        // siteName already available from the validated site object
        return EventResponse.builder()
                .eventId(updated.getEventId())
                .userId(updated.getUserId())
                .siteId(updated.getSiteId())
                .siteName(site.getName())
                .eventName(updated.getEventName())
                .description(updated.getDescription())
                .startDate(updated.getStartDate())
                .finishDate(updated.getFinishDate())
                .status(updated.getStatus())
                .isPublic(updated.getIsPublic())
                .availableSeats(updated.getAvailableSeats())
                .categoryId(resolvedCategoryId)
                .rejectionReason(updated.getRejectionReason())
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .createdBy(updated.getCreatedBy())
                .updatedBy(updated.getUpdatedBy())
                .build();
    }
}
