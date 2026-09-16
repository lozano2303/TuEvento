package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.category.application.dto.response.CategoryResponse;
import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitePort;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final GetSitePort           getSitePort;
    private final ProfileJpaRepository  profileRepository;
    private final LoginCredentialsJpaRepository loginCredentialsRepository;
    private final StoredFileRepository  storedFileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminEventSummaryResponse> getEvents(EventStatus status) {
        List<Event> events = (status == null)
                ? eventRepository.findAll()
                : eventRepository.findByStatus(status);

        return events.stream()
                .map(this::toAdminSummary)
                .toList();
    }

    private AdminEventSummaryResponse toAdminSummary(Event e) {

        // ── Site name ─────────────────────────────────────────────────────────
        String siteName = null;
        try {
            siteName = getSitePort.getSite(Math.toIntExact(e.getSiteId())).getName();
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

        // ── Organizer name (from profile) ─────────────────────────────────────
        String organizerName = null;
        try {
            organizerName = profileRepository
                    .findByUserId(Math.toIntExact(e.getUserId()))
                    .map(p -> p.getFullName())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve organizerName for event {}, userId {}: {}",
                    e.getEventId(), e.getUserId(), ex.getMessage());
        }

        // ── Organizer email (from login_credentials) ──────────────────────────
        String organizerEmail = null;
        try {
            organizerEmail = loginCredentialsRepository
                    .findByUserUserId(Math.toIntExact(e.getUserId()))
                    .map(c -> c.getEmail())
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve organizerEmail for event {}, userId {}: {}",
                    e.getEventId(), e.getUserId(), ex.getMessage());
        }

        // ── Organizer profile picture — lee publicUrl directamente desde stored_file,
        //    sin generar URL presignada (evita escritura de log dentro de readOnly tx) ──
        String organizerProfilePicture = null;
        try {
            var profileOpt = profileRepository.findByUserId(Math.toIntExact(e.getUserId()));
            if (profileOpt.isPresent()) {
                Integer avatarFileId = profileOpt.get().getStoredFileId();
                if (avatarFileId != null) {
                    organizerProfilePicture = storedFileRepository.findById(avatarFileId)
                            .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                            .map(f -> f.getPublicUrl())
                            .orElse(null);
                }
            }
        } catch (Exception ex) {
            log.warn("Admin events: could not resolve organizerProfilePicture for event {}, userId {}: {}",
                    e.getEventId(), e.getUserId(), ex.getMessage());
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
