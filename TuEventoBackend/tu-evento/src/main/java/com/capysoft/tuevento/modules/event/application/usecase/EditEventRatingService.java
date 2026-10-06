package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.application.port.in.EditEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Edita el texto de un comentario propio aplicando la regla B:
 * <ul>
 *   <li>Solo el autor puede editar.</li>
 *   <li>Solo se puede cambiar el texto (el rating no cambia).</li>
 *   <li>Solo dentro de las 2 horas desde createdAt (usa Clock inyectable).</li>
 *   <li>Editar no cuenta para el antispam (R5).</li>
 * </ul>
 *
 * <p>El payload publicado en /comments/updated y la respuesta del PATCH tienen
 * la MISMA forma que un elemento del GET /ratings, incluyendo isOrganizer correcto.
 */
@Service
@RequiredArgsConstructor
public class EditEventRatingService implements EditEventRatingUseCase {

    private static final long EDIT_WINDOW_HOURS = 2L;

    private final EventRatingRepository     ratingRepository;
    private final EventRepository           eventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileJpaRepository      profileJpaRepository;
    private final Clock                     clock;

    @Override
    @Transactional
    public EventRatingResponse execute(Long eventId, Long ratingId, Long userId, String comment) {

        EventRating existing = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException("RATING_NOT_FOUND",
                        "Rating not found with id: " + ratingId));

        if (!existing.getEventId().equals(eventId)) {
            throw new NotFoundException("RATING_NOT_FOUND",
                    "Rating " + ratingId + " does not belong to event " + eventId);
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("RATING_ACCESS_DENIED",
                    "User " + userId + " cannot edit rating " + ratingId);
        }

        // ── Regla B: ventana de 2 horas ──────────────────────────────────────
        LocalDateTime now = LocalDateTime.now(clock);
        long hoursSinceCreated = Duration.between(existing.getCreatedAt(), now).toHours();
        if (hoursSinceCreated >= EDIT_WINDOW_HOURS) {
            throw new BusinessException("COMMENT_EDIT_WINDOW_EXPIRED",
                    "The edit window of " + EDIT_WINDOW_HOURS
                            + " hours has passed for rating " + ratingId);
        }

        // ── Trim y validación del texto ──────────────────────────────────────
        String trimmed = comment == null ? null : comment.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new BusinessException("COMMENT_BLANK", "Comment text must not be blank");
        }
        if (trimmed.length() > 500) {
            throw new BusinessException("COMMENT_TOO_LONG",
                    "Comment must be 500 characters or fewer");
        }

        // ── Determinar si el autor es el organizador del evento (R4/isOrganizer) ─
        // Necesario para que el payload del WS y la respuesta REST tengan la misma
        // forma que un elemento del GET /ratings.
        Long organizerUserId = eventRepository.findById(eventId)
                .map(e -> e.getUserId())
                .orElse(null);
        boolean isOrganizer = organizerUserId != null && organizerUserId.equals(userId);

        // ── Guardar ──────────────────────────────────────────────────────────
        EventRating updated = ratingRepository.save(EventRating.builder()
                .ratingId(existing.getRatingId())
                .eventId(existing.getEventId())
                .userId(existing.getUserId())
                .rating(existing.getRating())
                .comment(trimmed)
                .isVisible(existing.getIsVisible())
                .createdAt(existing.getCreatedAt())
                .editedAt(now)
                .parentRatingId(existing.getParentRatingId())
                .build());

        // ── Resolver nombre del autor ────────────────────────────────────────
        String authorName = profileJpaRepository.findByUserId(userId.intValue())
                .map(p -> p.getFullName())
                .orElse("Usuario");

        // ── Publicar evento de dominio (AFTER_COMMIT → WebSocket) ────────────
        // Payload con la MISMA forma que un elemento del GET /ratings.
        eventPublisher.publishEvent(EventRatingUpdatedEvent.builder()
                .ratingId(updated.getRatingId())
                .eventId(eventId)
                .userId(updated.getUserId())
                .authorName(authorName)
                .rating(updated.getRating())
                .comment(updated.getComment())
                .isVisible(updated.getIsVisible())
                .isOrganizer(isOrganizer)
                .createdAt(updated.getCreatedAt())
                .editedAt(updated.getEditedAt())
                .parentRatingId(updated.getParentRatingId())
                .build());

        return EventRatingResponse.builder()
                .ratingId(updated.getRatingId())
                .userId(updated.getUserId())
                .authorName(authorName)
                .rating(updated.getRating())
                .comment(updated.getComment())
                .isVisible(updated.getIsVisible())
                .isOrganizer(isOrganizer)
                .createdAt(updated.getCreatedAt())
                .editedAt(updated.getEditedAt())
                .parentRatingId(updated.getParentRatingId())
                .editableUntil(updated.getCreatedAt() != null
                        ? updated.getCreatedAt().plusHours(EDIT_WINDOW_HOURS) : null)
                .build();
    }
}
