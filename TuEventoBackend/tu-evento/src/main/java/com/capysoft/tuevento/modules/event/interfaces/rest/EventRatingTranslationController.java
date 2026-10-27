package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.translation.TranslationAdminSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Administración de las traducciones de comentarios (ratings) de eventos. A diferencia de otros
 * listados, aquí "sin traducir" excluye los que ya se están generando, "por revisar" incluye las
 * traducciones en PENDING_REVIEW y un filtro desconocido muestra todo.
 */
@RestController
@RequestMapping("/api/v1/admin/comment-translations")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comment Translation Management", description = "CRUD de traducciones de comentarios de eventos")
public class EventRatingTranslationController {

    private static final String TRANSLATED_COMMENT = "translatedComment";

    private final EventRatingTranslationRepository eventRatingTranslationRepository;
    private final EventRatingRepository eventRatingRepository;
    private final LanguageCatalog languageCatalog;
    private final TranslationRequests translationRequests;
    private final EventRepository eventRepository;

    @GetMapping("/{languageCode}")
    @Operation(summary = "Obtener todas las traducciones de comentarios para un idioma")
    public ResponseEntity<List<Map<String, Object>>> getAllCommentTranslations(
            @PathVariable String languageCode,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = TranslationAdminSupport.FILTER_ALL) String filter) {

        Optional<LanguageSummary> language = languageCatalog.findByCode(languageCode);
        if (language.isEmpty()) {
            log.warn("Language not found: {}", languageCode);
            return ResponseEntity.badRequest().build();
        }
        if (eventId == null) {
            // Listar los comentarios de todos los eventos sería demasiado costoso
            log.warn("No event filter specified for comment translations");
            return ResponseEntity.ok(new ArrayList<>());
        }
        Integer languageId = language.get().getLanguageId();
        String eventName = eventRepository.findById(eventId).map(Event::getEventName).orElse("Evento desconocido");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (EventRating comment : eventRatingRepository.findByEventIdOrderByCreatedAtDesc(eventId)) {
            if (comment.getParentRatingId() != null) {
                continue; // solo comentarios principales, no respuestas
            }
            EventRatingTranslation translation = eventRatingTranslationRepository
                    .findByRatingAndLanguage(comment.getRatingId().intValue(), languageId).orElse(null);
            boolean generating = isGenerating(comment.getRatingId(), languageId);
            boolean needsReview = needsReview(comment, translation);
            if (included(filter, translation, generating, needsReview)) {
                // La marca solo se informa con el filtro de revisión, como en el resto del panel
                boolean flagged = TranslationAdminSupport.FILTER_NEEDS_REVIEW.equalsIgnoreCase(filter) && needsReview;
                rows.add(row(comment, eventName, translation, generating, flagged));
            }
        }

        log.debug("Returning {} comment translations for language {}", rows.size(), languageCode);
        return ResponseEntity.ok(rows);
    }

    @PutMapping("/{ratingId}/language/{languageId}")
    @Operation(summary = "Actualizar o crear traducción de un comentario")
    public ResponseEntity<Map<String, Object>> updateCommentTranslation(
            @PathVariable Long ratingId,
            @PathVariable Integer languageId,
            @RequestBody Map<String, Object> request) {

        String translatedComment = (String) request.get(TRANSLATED_COMMENT);
        if (translatedComment == null || translatedComment.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Translated comment is required"));
        }
        String status = (String) request.get("status");

        EventRatingTranslation translation = eventRatingTranslationRepository
                .findByRatingAndLanguage(ratingId.intValue(), languageId)
                .orElseGet(() -> {
                    EventRatingTranslation created = new EventRatingTranslation();
                    created.setRatingId(ratingId.intValue());
                    created.setLanguageId(languageId);
                    return created;
                });
        translation.setTranslatedComment(translatedComment);
        translation.setStatus(status != null ? TranslationStatus.valueOf(status) : TranslationStatus.PUBLISHED);
        EventRatingTranslation saved = eventRatingTranslationRepository.save(translation);

        Map<String, Object> response = new HashMap<>();
        response.put("translationId", saved.getTranslationId());
        response.put("ratingId", saved.getRatingId());
        response.put("languageId", saved.getLanguageId());
        response.put(TRANSLATED_COMMENT, saved.getTranslatedComment());
        response.put("status", saved.getStatus());
        log.info("Updated comment translation: id={}", saved.getTranslationId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ratingId}/language/{languageId}")
    @Operation(summary = "Eliminar traducción de un comentario")
    public ResponseEntity<Void> deleteCommentTranslation(
            @PathVariable Long ratingId,
            @PathVariable Integer languageId) {

        Optional<EventRatingTranslation> translation =
                eventRatingTranslationRepository.findByRatingAndLanguage(ratingId.intValue(), languageId);
        if (translation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        eventRatingTranslationRepository.delete(translation.get());
        log.info("Deleted comment translation for rating {} in language {}", ratingId, languageId);
        return ResponseEntity.noContent().build();
    }

    /** Hay un trabajo de traducción automática pendiente para ese comentario e idioma. */
    private boolean isGenerating(Long ratingId, Integer languageId) {
        return translationRequests.jobStatus("event_rating", ratingId, languageId)
                .map(status -> status == TranslationJobStatus.PENDING)
                .orElse(false);
    }

    /**
     * Pendiente de revisión: sin traducción, vacía, igual al original (sin distinguir mayúsculas
     * ni espacios) o marcada PENDING_REVIEW.
     */
    private static boolean needsReview(EventRating comment, EventRatingTranslation translation) {
        if (translation == null) {
            return true;
        }
        String translated = translation.getTranslatedComment();
        if (translated == null || translated.isBlank()) {
            return true;
        }
        String original = comment.getComment();
        return (original != null && original.trim().equalsIgnoreCase(translated.trim()))
                || translation.getStatus() == TranslationStatus.PENDING_REVIEW;
    }

    private static boolean included(String filter, EventRatingTranslation translation,
                                    boolean generating, boolean needsReview) {
        return switch (filter.toLowerCase()) {
            case TranslationAdminSupport.FILTER_NOT_TRANSLATED -> translation == null && !generating;
            case TranslationAdminSupport.FILTER_TRANSLATED -> translation != null
                    && translation.getTranslatedComment() != null && !translation.getTranslatedComment().isBlank();
            case TranslationAdminSupport.FILTER_NEEDS_REVIEW -> needsReview;
            default -> true; // "all" y filtros desconocidos
        };
    }

    private static Map<String, Object> row(EventRating comment, String eventName, EventRatingTranslation translation,
                                           boolean generating, boolean needsReview) {
        Map<String, Object> row = new HashMap<>();
        row.put("ratingId", comment.getRatingId());
        row.put("eventId", comment.getEventId());
        row.put("eventName", eventName);
        row.put("comment", comment.getComment());
        row.put("rating", comment.getRating());
        row.put("createdAt", comment.getCreatedAt());
        row.put("translationGenerating", generating);
        row.put("needsReview", needsReview);
        row.put("translationId", translation != null ? translation.getTranslationId() : null);
        row.put(TRANSLATED_COMMENT, translation != null ? translation.getTranslatedComment() : null);
        row.put("translationStatus", translation != null ? translation.getStatus() : null);
        return row;
    }
}
