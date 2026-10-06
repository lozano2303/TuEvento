package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.AddEventRatingRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AddEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
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
import java.time.LocalDateTime;

/**
 * Crea un comentario/rating de evento aplicando las reglas de negocio R1-R5.
 *
 * <ul>
 *   <li>R1: Cualquier usuario autenticado puede comentar.</li>
 *   <li>R2: Múltiples comentarios por persona y evento.</li>
 *   <li>R3: El primer comentario CON rating en el evento lleva calificación;
 *       los siguientes se guardan con rating = null.</li>
 *   <li>R4: El organizador del evento (event.userId == userId) siempre
 *       guarda rating = null, sin importar lo que envíe el cliente.</li>
 *   <li>R5: Antispam — máximo 1 comentario cada 10 segundos por
 *       persona/evento (COMMENT_RATE_LIMITED → 400).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AddEventRatingService implements AddEventRatingUseCase {

    private static final int ANTISPAM_SECONDS = 10;

    private final EventRepository          eventRepository;
    private final EventRatingRepository    ratingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileJpaRepository     profileJpaRepository;
    private final Clock                    clock;

    @Override
    @Transactional
    public EventRatingResponse execute(Long eventId, AddEventRatingRequest request, Long userId) {

        // ── Carga y validaciones de evento ──────────────────────────────────
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.COMPLETED && event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException("EVENT_RATING_NOT_ALLOWED",
                    "Comments are only allowed for PUBLISHED or COMPLETED events");
        }

        if (!Boolean.TRUE.equals(event.getIsPublic())) {
            throw new BusinessException("EVENT_RATING_NOT_ALLOWED",
                    "Comments are not allowed for private events");
        }

        // ── R5: antispam ────────────────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now(clock);
        ratingRepository.findLastByEventIdAndUserId(eventId, userId).ifPresent(last -> {
            long secondsSinceLast = java.time.Duration.between(last.getCreatedAt(), now).getSeconds();
            if (secondsSinceLast < ANTISPAM_SECONDS) {
                throw new BusinessException("COMMENT_RATE_LIMITED",
                        "Please wait " + (ANTISPAM_SECONDS - secondsSinceLast)
                                + " second(s) before commenting again");
            }
        });

        // ── Trim y validación de texto ───────────────────────────────────────
        String trimmedComment = request.getComment() == null ? null : request.getComment().trim();
        if (trimmedComment == null || trimmedComment.isEmpty()) {
            throw new BusinessException("COMMENT_BLANK",
                    "Comment text must not be blank");
        }
        if (trimmedComment.length() > 500) {
            throw new BusinessException("COMMENT_TOO_LONG",
                    "Comment must be 500 characters or fewer");
        }

        // ── R4: el organizador nunca califica ────────────────────────────────
        boolean isOrganizer = event.getUserId().equals(userId);

        // ── R3: calificación solo si es el primero en tenerla ────────────────
        // Si es organizador → null. Si ya tiene uno con rating → null. Si no → usar lo enviado.
        Integer effectiveRating = null;
        if (!isOrganizer) {
            boolean alreadyHasRating = ratingRepository.existsRatedCommentByEventIdAndUserId(eventId, userId);
            if (!alreadyHasRating) {
                effectiveRating = request.getRating(); // puede ser null si el cliente no lo envió
            }
            // Si alreadyHasRating == true → effectiveRating permanece null (ignoramos lo enviado)
        }

        // ── Guardar ──────────────────────────────────────────────────────────
        EventRating saved = ratingRepository.save(EventRating.builder()
                .eventId(eventId)
                .userId(userId)
                .rating(effectiveRating)
                .comment(trimmedComment)
                .isVisible(true)
                .createdAt(now)
                .build());

        // ── Resolver nombre del autor ────────────────────────────────────────
        String authorName = profileJpaRepository.findByUserId(userId.intValue())
                .map(p -> p.getFullName())
                .orElse("Usuario");

        // ── Publicar evento de dominio (AFTER_COMMIT lo enviará al WS) ───────
        eventPublisher.publishEvent(EventRatingAddedEvent.builder()
                .ratingId(saved.getRatingId())
                .eventId(eventId)
                .userId(userId)
                .rating(saved.getRating())
                .comment(saved.getComment())
                .isVisible(saved.getIsVisible())
                .isOrganizer(isOrganizer)
                .occurredAt(saved.getCreatedAt())
                .build());

        return EventRatingResponse.builder()
                .ratingId(saved.getRatingId())
                .userId(saved.getUserId())
                .authorName(authorName)
                .rating(saved.getRating())
                .comment(saved.getComment())
                .isVisible(saved.getIsVisible())
                .isOrganizer(isOrganizer)
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
