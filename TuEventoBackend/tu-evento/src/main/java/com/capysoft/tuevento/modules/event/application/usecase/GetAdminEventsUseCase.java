package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort;
import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort.UserRef;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Admin-only use case: lists all events regardless of status or visibility.
 * Resolves organizer name, site name, category name and cover URL per event
 * using the same fail-soft pattern as GetEventService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetAdminEventsUseCase implements GetAdminEventsPort {

    private final EventRepository       eventRepository;
    private final EventMediaRepository  eventMediaRepository;
    private final CategoryEventUseCase  categoryEventUseCase;
    private final SiteLookupPort        siteLookupPort;
    private final UserDirectoryPort     userDirectoryPort;
    private final StoredFileRepository  storedFileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminEventSummaryResponse> getEvents(EventStatus status) {
        List<Event> events = (status == null)
                ? eventRepository.findAll()
                : eventRepository.findByStatus(status);

        Map<Integer, UserRef> organizers = findOrganizers(events);
        return events.stream()
                .map(e -> toAdminSummary(e, organizers))
                .toList();
    }

    /** Todos los organizadores de la lista en una sola consulta, en vez de tres por evento. */
    private Map<Integer, UserRef> findOrganizers(List<Event> events) {
        List<Integer> organizerIds = events.stream()
                .map(Event::getUserId)
                .filter(Objects::nonNull)
                .map(Math::toIntExact)
                .distinct()
                .toList();
        try {
            return userDirectoryPort.findUsers(organizerIds);
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve organizers: {}", ex.getMessage());
            return Map.of();
        }
    }

    private AdminEventSummaryResponse toAdminSummary(Event e, Map<Integer, UserRef> organizers) {

        // ── Site name ─────────────────────────────────────────────────────────
        String siteName = null;
        try {
            siteName = siteLookupPort.getSite(Math.toIntExact(e.getSiteId())).name();
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve siteName for event {}: {}",
                    e.getEventId(), ex.getMessage());
        }

        // ── Category ──────────────────────────────────────────────────────────
        Integer categoryId   = null;
        String  categoryName = null;
        try {
            List<CategoryResponse> cats = categoryEventUseCase
                    .getCategoriesByEvent(Math.toIntExact(e.getEventId()));
            if (!cats.isEmpty()) {
                categoryId   = cats.get(0).getCategoryId();
                categoryName = cats.get(0).getName();
            }
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve category for event {}: {}",
                    e.getEventId(), ex.getMessage());
        }

        // ── Cover URL (first uploaded media) ──────────────────────────────────
        String coverUrl = null;
        try {
            coverUrl = eventMediaRepository.findFirstByEventId(e.getEventId())
                    .map(media -> media.getImgUrl())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve coverUrl for event {}: {}",
                    e.getEventId(), ex.getMessage());
        }

        // ── Organizer (nombre, email y foto, de Identidad) ────────────────────
        UserRef organizer = e.getUserId() == null ? null : organizers.get(Math.toIntExact(e.getUserId()));
        String organizerName  = organizer == null ? null : organizer.fullName();
        String organizerEmail = organizer == null ? null : organizer.email();

        // ── Organizer profile picture — lee publicUrl directamente desde stored_file,
        //    sin generar URL presignada (evita escritura de log dentro de readOnly tx) ──
        String organizerProfilePicture = null;
        if (organizer != null && organizer.avatarFileId() != null) {
            try {
                organizerProfilePicture = storedFileRepository.findById(organizer.avatarFileId())
                        .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                        .map(f -> f.getPublicUrl())
                        .orElse(null);
            } catch (Exception ex) {
                log.warn("Admin events: could not resolve organizerProfilePicture for event {}, userId {}: {}",
                        e.getEventId(), e.getUserId(), ex.getMessage());
            }
        }

        return AdminEventSummaryResponse.builder()
                .eventId(e.getEventId())
                .eventName(e.getEventName())
                .status(e.getStatus())
                .startDate(e.getStartDate())
                .finishDate(e.getFinishDate())
                .isPublic(e.getIsPublic())
                .availableSeats(e.getAvailableSeats())
                .siteName(siteName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .coverUrl(coverUrl)
                .organizerName(organizerName)
                .organizerUserId(e.getUserId())
                .organizerEmail(organizerEmail)
                .organizerProfilePicture(organizerProfilePicture)
                .build();
    }
}
