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
 * Crea un comentario/rating de evento aplicando las reglas de negocio R1-R5
 * y la regla de respuestas (A).
 *
 * <ul>
 *   <li>R1: Cualquier usuario autenticado puede comentar o responder.</li>
 *   <li>R2: Múltiples comentarios por persona y evento.</li>
 *   <li>R3: El primer comentario PRINCIPAL con rating lleva calificación;
 *       los siguientes se guardan con rating = null. Las respuestas siempre null.</li>
 *   <li>R4: El organizador del evento siempre rating = null.</li>
 *   <li>R5: Antispam — máximo 1 comentario/respuesta cada 10 segundos
 *       por persona/evento (COMMENT_RATE_LIMITED → 400).</li>
 *   <li>A:  Si hay parentRatingId, el padre debe existir, ser del mismo evento
 *       y ser un comentario principal (parent_rating_id IS NULL en el padre).
 *       Si el padre es una respuesta → COMMENT_PARENT_INVALID.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AddEventRatingService implements AddEventRatingUseCase {

    private static final int ANTISPAM_SECONDS = 10;

    private final EventRepository           eventRepository;
    private final EventRatingRepository     ratingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileJpaRepository      profileJpaRepository;
    private final Clock                     clock;

    @Override
    @Transactional
    public EventRatingResponse execute(Long eventId, AddEventRatingRequest request, Long userId) {

        // ── Carga y validaciones del evento ─────────────────────────────────
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

        // ── Regla A: validación del padre ────────────────────────────────────
        Long parentRatingId = request.getParentRatingId();
        if (parentRatingId != null) {
            EventRating parent = ratingRepository.findById(parentRatingId)
                    .orElseThrow(() -> new NotFoundException("RATING_NOT_FOUND",
                            "Parent rating not found with id: " + parentRatingId));
            if (!parent.getEventId().equals(eventId)) {
                throw new BusinessException("COMMENT_PARENT_INVALID",
                        "Parent comment does not belong to this event");
            }
            if (parent.getParentRatingId() != null) {
                throw new BusinessException("COMMENT_PARENT_INVALID",
                        "Cannot reply to a reply — only one level of nesting is allowed");
            }
        }

        // ── R5: antispam ─────────────────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now(clock);
        ratingRepository.findLastByEventIdAndUserId(eventId, userId).ifPresent(last -> {
            long secondsSinceLast = java.time.Duration.between(last.getCreatedAt(), now).getSeconds();
            if (secondsSinceLast < ANTISPAM_SECONDS) {
                throw new BusinessException("COMMENT_RATE_LIMITED",
                        "Please wait " + (ANTISPAM_SECONDS - secondsSinceLast)
                                + " second(s) before commenting again");
            }
        });

        // ── Trim y validación del texto ──────────────────────────────────────
        String trimmedComment = request.getComment() == null ? null : request.getComment().trim();
        if (trimmedComment == null || trimmedComment.isEmpty()) {
            throw new BusinessException("COMMENT_BLANK", "Comment text must not be blank");
        }
        if (trimmedComment.length() > 500) {
            throw new BusinessException("COMMENT_TOO_LONG",
                    "Comment must be 500 characters or fewer");
        }

        // ── R4: el organizador nunca califica ────────────────────────────────
        boolean isOrganizer = event.getUserId().equals(userId);

        // ── R3 + regla A: las respuestas siempre llevan rating null ──────────
        // Un comentario principal lleva rating solo si el usuario no es el
        // organizador y aún no tiene un comentario principal con rating.
        // En ese caso la calificación es OBLIGATORIA (1-5).
        // Si es una respuesta, el rating se ignora completamente → null.
        Integer effectiveRating = null;
        if (parentRatingId == null && !isOrganizer) {
            boolean alreadyHasRating =
                    ratingRepository.existsRatedCommentByEventIdAndUserId(eventId, userId);
            if (!alreadyHasRating) {
                Integer providedRating = request.getRating();
                if (providedRating == null) {
                    throw new BusinessException("COMMENT_RATING_REQUIRED",
                            "A rating between 1 and 5 is required for your first comment on this event");
                }
                if (providedRating < 1 || providedRating > 5) {
                    throw new BusinessException("COMMENT_RATING_INVALID",
                            "Rating must be between 1 and 5");
                }
                effectiveRating = providedRating;
            }
            // Si ya tiene un comentario principal con rating, el nuevo se guarda sin calificación (R3).
        }
        // Respuestas: el rating enviado se ignora, siempre null (regla A).

        // ── Mención: resolver replyToUserId y replyToUserName ────────────────
        Long replyToUserId = request.getReplyToUserId();
        String replyToUserName = null;
        
        // Solo procesar mención si es una respuesta y el replyToUserId no es el mismo usuario
        if (parentRatingId != null && replyToUserId != null && !replyToUserId.equals(userId)) {
            // Captura final para uso dentro del lambda
            final Long replyToUserIdFinal = replyToUserId;
            // Validar que el usuario mencionado existe y tiene un comentario en este hilo
            boolean replyToUserExistsInThread = ratingRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                    .stream()
                    .anyMatch(r -> r.getUserId().equals(replyToUserIdFinal) && 
                                 (r.getRatingId().equals(parentRatingId) || 
                                  (r.getParentRatingId() != null && r.getParentRatingId().equals(parentRatingId))));
            
            if (replyToUserExistsInThread) {
                replyToUserName = profileJpaRepository.findByUserId(replyToUserId.intValue())
                        .map(p -> p.getFullName())
                        .orElse(null);
            } else {
                // Si no existe en el hilo, ignorar la mención
                replyToUserId = null;
            }
        } else if (replyToUserId != null && replyToUserId.equals(userId)) {
            // No permitir mención a sí mismo
            replyToUserId = null;
        }

        // ── Guardar ──────────────────────────────────────────────────────────
        EventRating saved = ratingRepository.save(EventRating.builder()
                .eventId(eventId)
                .userId(userId)
                .rating(effectiveRating)
                .comment(trimmedComment)
                .isVisible(true)
                .createdAt(now)
                .parentRatingId(parentRatingId)
                .replyToUserId(replyToUserId)
                .replyToUserName(replyToUserName)
                .build());

        // ── Resolver nombre del autor ────────────────────────────────────────
        String authorName = profileJpaRepository.findByUserId(userId.intValue())
                .map(p -> p.getFullName())
                .orElse("Usuario");

        // ── Publicar evento de dominio (AFTER_COMMIT → WebSocket) ────────────
        eventPublisher.publishEvent(EventRatingAddedEvent.builder()
                .ratingId(saved.getRatingId())
                .eventId(eventId)
                .userId(userId)
                .rating(saved.getRating())
                .comment(saved.getComment())
                .isVisible(saved.getIsVisible())
                .isOrganizer(isOrganizer)
                .parentRatingId(parentRatingId)
                .replyToUserId(saved.getReplyToUserId())
                .replyToUserName(saved.getReplyToUserName())
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
                .parentRatingId(saved.getParentRatingId())
                .editableUntil(saved.getCreatedAt() != null
                        ? saved.getCreatedAt().plusHours(2) : null)
                .replyToUserId(saved.getReplyToUserId())
                .replyToUserName(saved.getReplyToUserName())
                .build();
    }
}
