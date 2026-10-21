package com.capysoft.tuevento.modules.language.interfaces.rest;

import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventEntity;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.repository.EventJpaRepository;
import com.capysoft.tuevento.modules.language.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/comment-translations")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comment Translation Management", description = "CRUD de traducciones de comentarios de eventos")
public class EventRatingTranslationController {

    private final EventRatingTranslationRepository eventRatingTranslationRepository;
    private final EventRatingRepository eventRatingRepository;
    private final LanguageRepository languageRepository;
    private final TranslationJobRepository translationJobRepository;
    private final EventJpaRepository eventJpaRepository;

    @GetMapping("/{languageCode}")
    @Operation(summary = "Obtener todas las traducciones de comentarios para un idioma")
    public ResponseEntity<List<Map<String, Object>>> getAllCommentTranslations(
            @PathVariable String languageCode,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "all") String filter) {
        
        log.debug("Getting comment translations for language: {}, event: {}, filter: {}", languageCode, eventId, filter);
        
        Optional<Language> language = languageRepository.findByCode(languageCode);
        if (language.isEmpty()) {
            log.warn("Language not found: {}", languageCode);
            return ResponseEntity.badRequest().build();
        }
        
        Integer languageId = language.get().getLanguageId();
        
        // Obtener todos los comentarios
        List<EventRating> allComments = new ArrayList<>();
        if (eventId != null) {
            // Filtrar por evento específico
            allComments = eventRatingRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        } else {
            // Si no se especifica evento, retornar vacío por ahora
            // (obtener todos los comentarios de todos los eventos sería muy costoso)
            log.warn("No event filter specified for comment translations");
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        // Filtrar solo comentarios principales (no respuestas)
        List<EventRating> mainComments = allComments.stream()
                .filter(comment -> comment.getParentRatingId() == null)
                .collect(Collectors.toList());
        
        // Obtener traducciones existentes
        List<Integer> commentIds = mainComments.stream()
                .map(c -> c.getRatingId().intValue())
                .collect(Collectors.toList());
        
        List<EventRatingTranslation> translations = commentIds.isEmpty() 
                ? new ArrayList<>()
                : mainComments.stream()
                    .map(c -> eventRatingTranslationRepository.findByRatingAndLanguage(c.getRatingId().intValue(), languageId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        
        Map<Integer, EventRatingTranslation> translationMap = translations.stream()
                .collect(Collectors.toMap(EventRatingTranslation::getRatingId, t -> t));
        
        // Obtener jobs pendientes para saber cuáles están generando traducción
        Set<Long> generatingCommentIds = new HashSet<>();
        for (EventRating comment : mainComments) {
            Optional<TranslationJob> job = translationJobRepository.findByEntityAndTargetLanguage(
                    "event_rating", 
                    comment.getRatingId().intValue(), 
                    languageId
            );
            if (job.isPresent() && job.get().getStatus() == TranslationJobStatus.PENDING) {
                generatingCommentIds.add(comment.getRatingId());
            }
        }
        
        // Construir respuesta
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (EventRating comment : mainComments) {
            EventRatingTranslation translation = translationMap.get(comment.getRatingId().intValue());
            boolean isGenerating = generatingCommentIds.contains(comment.getRatingId());
            
            // Aplicar filtros
            boolean includeInResult = false;
            boolean needsReview = false;
            
            switch (filter.toLowerCase()) {
                case "all":
                    includeInResult = true;
                    break;
                    
                case "not-translated":
                    // Sin traducción y no está generando
                    includeInResult = translation == null && !isGenerating;
                    break;
                    
                case "translated":
                    // Tiene traducción con contenido válido
                    includeInResult = translation != null && 
                                     translation.getTranslatedComment() != null && 
                                     !translation.getTranslatedComment().trim().isEmpty();
                    break;
                    
                case "needs-review":
                    // Sin traducción
                    if (translation == null) {
                        includeInResult = true;
                        needsReview = true;
                    } else {
                        String original = comment.getComment();
                        String translated = translation.getTranslatedComment();
                        
                        // Traducción vacía
                        if (translated == null || translated.trim().isEmpty()) {
                            includeInResult = true;
                            needsReview = true;
                        } 
                        // Traducción idéntica al original (exacta)
                        else if (original != null && original.equals(translated)) {
                            includeInResult = true;
                            needsReview = true;
                        } 
                        // Traducción idéntica al original (case insensitive)
                        else if (original != null && original.toLowerCase().trim().equals(translated.toLowerCase().trim())) {
                            includeInResult = true;
                            needsReview = true;
                        }
                        // Estado PENDING_REVIEW
                        else if (translation.getStatus() == TranslationStatus.PENDING_REVIEW) {
                            includeInResult = true;
                            needsReview = true;
                        }
                    }
                    break;
                    
                default:
                    includeInResult = true;
                    break;
            }
            
            if (!includeInResult) {
                continue;
            }
            
            // Obtener información del evento
            EventEntity event = eventJpaRepository.findById(comment.getEventId()).orElse(null);
            
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("ratingId", comment.getRatingId());
            commentData.put("eventId", comment.getEventId());
            commentData.put("eventName", event != null ? event.getEventName() : "Evento desconocido");
            commentData.put("comment", comment.getComment());
            commentData.put("rating", comment.getRating());
            commentData.put("createdAt", comment.getCreatedAt());
            commentData.put("translationGenerating", isGenerating);
            commentData.put("needsReview", needsReview);
            
            if (translation != null) {
                commentData.put("translationId", translation.getTranslationId());
                commentData.put("translatedComment", translation.getTranslatedComment());
                commentData.put("translationStatus", translation.getStatus());
            } else {
                commentData.put("translationId", null);
                commentData.put("translatedComment", null);
                commentData.put("translationStatus", null);
            }
            
            result.add(commentData);
        }
        
        log.debug("Returning {} comment translations for language {}", result.size(), languageCode);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{ratingId}/language/{languageId}")
    @Operation(summary = "Actualizar o crear traducción de un comentario")
    public ResponseEntity<Map<String, Object>> updateCommentTranslation(
            @PathVariable Long ratingId,
            @PathVariable Integer languageId,
            @RequestBody Map<String, Object> request) {
        
        log.debug("Updating comment translation: ratingId={}, languageId={}", ratingId, languageId);
        
        String translatedComment = (String) request.get("translatedComment");
        String statusStr = (String) request.get("status");
        
        if (translatedComment == null || translatedComment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Translated comment is required"));
        }
        
        TranslationStatus status = statusStr != null 
                ? TranslationStatus.valueOf(statusStr) 
                : TranslationStatus.PUBLISHED;
        
        Optional<EventRatingTranslation> existingOpt = eventRatingTranslationRepository
                .findByRatingAndLanguage(ratingId.intValue(), languageId);
        
        EventRatingTranslation translation;
        if (existingOpt.isPresent()) {
            translation = existingOpt.get();
            translation.setTranslatedComment(translatedComment);
            translation.setStatus(status);
        } else {
            translation = new EventRatingTranslation();
            translation.setRatingId(ratingId.intValue());
            translation.setLanguageId(languageId);
            translation.setTranslatedComment(translatedComment);
            translation.setStatus(status);
        }
        
        // Siempre marcar como MANUAL cuando el admin edita
        EventRatingTranslation saved = eventRatingTranslationRepository.save(translation);
        
        Map<String, Object> response = new HashMap<>();
        response.put("translationId", saved.getTranslationId());
        response.put("ratingId", saved.getRatingId());
        response.put("languageId", saved.getLanguageId());
        response.put("translatedComment", saved.getTranslatedComment());
        response.put("status", saved.getStatus());
        
        log.info("Updated comment translation: id={}", saved.getTranslationId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ratingId}/language/{languageId}")
    @Operation(summary = "Eliminar traducción de un comentario")
    public ResponseEntity<Void> deleteCommentTranslation(
            @PathVariable Long ratingId,
            @PathVariable Integer languageId) {
        
        log.debug("Deleting comment translation: ratingId={}, languageId={}", ratingId, languageId);
        
        Optional<EventRatingTranslation> translation = eventRatingTranslationRepository
                .findByRatingAndLanguage(ratingId.intValue(), languageId);
        
        if (translation.isPresent()) {
            eventRatingTranslationRepository.delete(translation.get());
            log.info("Deleted comment translation for rating {} in language {}", ratingId, languageId);
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.notFound().build();
    }
}
