package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetEventUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetEventService implements GetEventUseCase {

    private final EventRepository      eventRepository;
    private final CategoryEventUseCase categoryEventUseCase;
    private final SiteLookupPort       siteLookupPort;
    private final EventMediaRepository eventMediaRepository;
    private final com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository ticketRepository;

    // Translation support
    private final com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository eventTranslationRepository;
    private final LanguageCatalog languageCatalog;
    private final TranslationRequests translationRequests;

    // ─── Authenticated queries ────────────────────────────────────────────────

    @Override
    public EventResponse findById(Long eventId) {
        return findById(eventId, null);
    }

    @Override
    public EventResponse findById(Long eventId, String languageCode) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));
        return toResponse(event, languageCode);
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

    // ─── Public queries ───────────────────────────────────────────────────────

    @Override
    public List<EventSummaryResponse> getPublishedEvents() {
        return eventRepository.findAllPublished().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<EventResponse> getPublishedEventsWithTranslations(String languageCode) {
        return eventRepository.findAllPublished().stream()
                .map(event -> toResponse(event, languageCode))
                .toList();
    }

    @Override
    public List<EventSummaryResponse> getPublishedEventsByCityId(Long cityId) {
        return eventRepository.findByCityId(cityId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> getPublishedEventsByCategoryId(Integer categoryId) {
        return eventRepository.findByCategoryId(categoryId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> getPublishedEventsByDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("INVALID_DATE_RANGE",
                    "from must be before or equal to to");
        }
        return eventRepository.findByDateRange(from, to).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<EventSummaryResponse> searchPublishedEvents(String q, LocalDate date) {
        if (date != null && date.isBefore(LocalDate.now())) {
            throw new BusinessException("INVALID_SEARCH_DATE",
                    "Search date cannot be in the past");
        }
        // blank query → empty string so LIKE '%%' matches all event names
        String term = (q == null || q.isBlank()) ? "" : q.trim();

        // Split into two separate repo calls to avoid null-binding issues in
        // native queries on PostgreSQL when date is null.
        List<Event> results = (date == null)
                ? eventRepository.searchPublishedByText(term)
                : eventRepository.searchPublishedByTextAndDate(term, date);

        return results.stream().map(this::toSummary).toList();
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private EventResponse toResponse(Event e) {
        return toResponse(e, null);
    }

    private EventResponse toResponse(Event e, String languageCode) {
        Integer categoryId = null;
        try {
            List<CategoryResponse> cats = categoryEventUseCase.getCategoriesByEvent(
                    Math.toIntExact(e.getEventId()));
            categoryId = cats.isEmpty() ? null : cats.get(0).getCategoryId();
        } catch (Exception ex) {
            log.warn("Could not resolve categoryId for event {}: {}", e.getEventId(), ex.getMessage());
        }

        String siteName = null;
        try {
            siteName = siteLookupPort.getSite(Math.toIntExact(e.getSiteId())).name();
        } catch (Exception ex) {
            log.warn("Could not resolve siteName for event {}: {}", e.getEventId(), ex.getMessage());
        }

        // Primera imagen del evento (media_id más bajo) — portada del listado público.
        // null si el evento aún no tiene imágenes (datos legacy o borrador).
        String coverUrl = null;
        try {
            coverUrl = eventMediaRepository.findFirstByEventId(e.getEventId())
                    .map(media -> media.getImgUrl())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Could not resolve coverUrl for event {}: {}", e.getEventId(), ex.getMessage());
        }

        // Translation support with LAZY LOADING
        String translatedName = null;
        String translatedDescription = null;
        String actualLanguageCode = null;
        Boolean translationGenerating = false;

        if (languageCode != null && !languageCode.isBlank()) {
            try {
                // IMPORTANT: Don't translate if requested language is the same as source language (Spanish)
                // Events are created in Spanish by default
                if ("es".equalsIgnoreCase(languageCode)) {
                    // Return original content without translation for Spanish
                    log.debug("Requested language is Spanish (source language) - no translation needed for event {}", 
                            e.getEventId());
                } else {
                    var language = languageCatalog.findByCode(languageCode);
                    if (language.isPresent()) {
                        var translation = eventTranslationRepository.findByEventAndLanguage(
                                Math.toIntExact(e.getEventId()), language.get().getLanguageId());
                        
                        // Mostrar traducción si existe (DRAFT o PUBLISHED)
                        if (translation.isPresent()) {
                            translatedName = translation.get().getTranslatedName();
                            translatedDescription = translation.get().getTranslatedDescription();
                            actualLanguageCode = languageCode;
                        } else {
                            translationGenerating = translationRequests.ensureRequested(
                                    "event", e.getEventId(), language.get().getLanguageId());
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not resolve translation for event {} language {}: {}",
                        e.getEventId(), languageCode, ex.getMessage());
            }
        }

        return EventResponse.builder()
                .eventId(e.getEventId())
                .userId(e.getUserId())
                .siteId(e.getSiteId())
                .siteName(siteName)
                .eventName(e.getEventName())
                .description(e.getDescription())
                .startDate(e.getStartDate())
                .finishDate(e.getFinishDate())
                .status(e.getStatus())
                .isPublic(e.getIsPublic())
                .availableSeats(e.getAvailableSeats())
                .categoryId(categoryId)
                .ticketsSold(countSold(e))
                .coverUrl(coverUrl)
                .translatedName(translatedName)
                .translatedDescription(translatedDescription)
                .languageCode(actualLanguageCode)
                .translationGenerating(translationGenerating)
                .rejectionReason(e.getRejectionReason())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private long countSold(Event e) {
        try {
            return ticketRepository.countSoldByEventId(e.getEventId());
        } catch (Exception ex) {
            log.warn("Could not count sold tickets for event {}: {}", e.getEventId(), ex.getMessage());
            return 0;
        }
    }

    private EventSummaryResponse toSummary(Event e) {
        String siteName = null;
        try {
            siteName = siteLookupPort.getSite(Math.toIntExact(e.getSiteId())).name();
        } catch (Exception ex) {
            log.warn("Could not resolve siteName for event {}: {}", e.getEventId(), ex.getMessage());
        }

        Integer categoryId = null;
        try {
            List<CategoryResponse> cats = categoryEventUseCase.getCategoriesByEvent(
                    Math.toIntExact(e.getEventId()));
            categoryId = cats.isEmpty() ? null : cats.get(0).getCategoryId();
        } catch (Exception ex) {
            log.warn("Could not resolve categoryId for event {}: {}", e.getEventId(), ex.getMessage());
        }

        // Primera imagen del evento (media_id más bajo) — portada del listado público.
        // null si el evento aún no tiene imágenes (datos legacy o borrador).
        String coverUrl = null;
        try {
            coverUrl = eventMediaRepository.findFirstByEventId(e.getEventId())
                    .map(media -> media.getImgUrl())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Could not resolve coverUrl for event {}: {}", e.getEventId(), ex.getMessage());
        }

        return EventSummaryResponse.builder()
                .eventId(e.getEventId())
                .eventName(e.getEventName())
                .status(e.getStatus())
                .startDate(e.getStartDate())
                .finishDate(e.getFinishDate())
                .isPublic(e.getIsPublic())
                .availableSeats(e.getAvailableSeats())
                .siteName(siteName)
                .categoryId(categoryId)
                .ticketsSold(countSold(e))
                .coverUrl(coverUrl)
                .build();
    }
}
