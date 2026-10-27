package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import com.capysoft.tuevento.shared.interfaces.translation.TranslationAdminSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller para gestionar traducciones de eventos desde el panel de administración. Las reglas
 * del listado y del cuerpo de alta/edición son las comunes de {@link TranslationAdminSupport}.
 */
@RestController
@RequestMapping("/api/v1/admin/event-translations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Translation Management", description = "CRUD de traducciones de eventos")
public class EventTranslationController {

    private final EventTranslationRepository eventTranslationRepository;
    private final EventRepository eventRepository;
    private final LanguageRepository languageRepository;

    @Operation(summary = "Get all events with translations for a language",
               description = "Lista todos los eventos del sistema con sus traducciones (si existen) para el idioma especificado. " +
                           "Puede filtrar para mostrar solo los que necesitan revisión.")
    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEventsWithTranslations(
            @Parameter(description = "Language code (en, fr, pt)", example = "en")
            @PathVariable String languageCode,

            @Parameter(description = "Filter: all, needs-review, translated, not-translated", example = "all")
            @RequestParam(defaultValue = TranslationAdminSupport.FILTER_ALL) String filter) {

        try {
            Optional<Language> language = languageRepository.findByCode(languageCode.toLowerCase());
            if (language.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Language not found: " + languageCode));
            }
            Integer languageId = language.get().getLanguageId();

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Event event : eventRepository.findAll()) {
                EventTranslation translation = eventTranslationRepository
                        .findByEventAndLanguage(event.getEventId().intValue(), languageId)
                        .orElse(null);
                String translatedName = translation != null ? translation.getTranslatedName() : null;
                var match = TranslationAdminSupport.match(filter, event.getEventName(), translatedName);
                if (match.included()) {
                    rows.add(row(event, translation, match.needsReview()));
                }
            }

            log.info("Retrieved {} events with translations for language {}", rows.size(), languageCode);
            return ResponseEntity.ok(ApiResponse.ok("Events with translations retrieved", rows));

        } catch (Exception e) {
            log.error("Error getting events with translations for language: {}", languageCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving events: " + e.getMessage()));
        }
    }

    @Operation(summary = "Create or update event translation",
               description = "Crea una nueva traducción o actualiza una existente para un evento en un idioma específico")
    @PutMapping("/{eventId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EventTranslation>> createOrUpdateTranslation(
            @Parameter(description = "Event ID", example = "1")
            @PathVariable Integer eventId,

            @Parameter(description = "Language ID (2=en, 3=fr, 4=pt)", example = "2")
            @PathVariable Integer languageId,

            @RequestBody Map<String, String> translationData) {

        try {
            String invalid = invalidTarget(eventId, languageId);
            if (invalid != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(invalid));
            }

            EventTranslation translation = eventTranslationRepository.findByEventAndLanguage(eventId, languageId)
                    .orElse(EventTranslation.builder()
                            .eventId(eventId)
                            .languageId(languageId)
                            .source(TranslationSource.MANUAL)
                            .status(TranslationStatus.DRAFT)
                            .build());
            String translatedName = translationData.get("translatedName");
            if (translatedName != null) {
                translation.setTranslatedName(translatedName.trim());
            }
            String translatedDescription = translationData.get("translatedDescription");
            if (translatedDescription != null) {
                translation.setTranslatedDescription(translatedDescription.trim());
            }
            translation.setSource(TranslationAdminSupport.sourceOf(translationData));
            translation.setStatus(TranslationAdminSupport.statusOf(translationData));

            EventTranslation saved = eventTranslationRepository.save(translation);
            log.info("Translation saved for event {} in language {} by admin", eventId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation saved successfully", saved));

        } catch (Exception e) {
            log.error("Error saving event translation for event {} in language {}", eventId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error saving translation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get specific event translation",
               description = "Obtiene la traducción de un evento para un idioma específico")
    @GetMapping("/{eventId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EventTranslation>> getTranslation(
            @Parameter(description = "Event ID") @PathVariable Integer eventId,
            @Parameter(description = "Language ID") @PathVariable Integer languageId) {

        return eventTranslationRepository.findByEventAndLanguage(eventId, languageId)
                .map(t -> ResponseEntity.ok(ApiResponse.ok("Translation retrieved", t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete event translation",
               description = "Elimina la traducción de un evento para un idioma específico")
    @DeleteMapping("/{eventId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTranslation(
            @Parameter(description = "Event ID") @PathVariable Integer eventId,
            @Parameter(description = "Language ID") @PathVariable Integer languageId) {

        try {
            Optional<EventTranslation> existing = eventTranslationRepository.findByEventAndLanguage(eventId, languageId);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            eventTranslationRepository.delete(existing.get());
            log.info("Translation deleted for event {} in language {}", eventId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting translation for event {} in language {}", eventId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting translation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get all translations for an event",
               description = "Obtiene todas las traducciones de un evento en todos los idiomas")
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<EventTranslation>>> getAllTranslationsForEvent(
            @Parameter(description = "Event ID") @PathVariable Integer eventId) {

        try {
            if (eventRepository.findById(eventId.longValue()).isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Event not found with ID: " + eventId));
            }
            return ResponseEntity.ok(ApiResponse.ok("Translations retrieved for event " + eventId,
                    eventTranslationRepository.findByEvent(eventId)));
        } catch (Exception e) {
            log.error("Error getting all translations for event {}", eventId, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /** Motivo por el que no se puede traducir ese evento a ese idioma, o {@code null} si se puede. */
    private String invalidTarget(Integer eventId, Integer languageId) {
        if (eventRepository.findById(eventId.longValue()).isEmpty()) {
            return "Event not found with ID: " + eventId;
        }
        Optional<Language> language = languageRepository.findById(languageId);
        if (language.isEmpty()) {
            return "Language not found with ID: " + languageId;
        }
        return "es".equalsIgnoreCase(language.get().getCode())
                ? "Cannot create translation for source language (Spanish)" : null;
    }

    /** Fila del listado; {@code translation} es {@code null} si el evento no tiene traducción. */
    private static Map<String, Object> row(Event event, EventTranslation translation, boolean needsReview) {
        Map<String, Object> row = new HashMap<>();
        row.put("eventId", event.getEventId());
        row.put("eventName", event.getEventName());
        row.put("description", event.getDescription() != null ? event.getDescription() : "");
        row.put("categoryName", ""); // la categoría es una relación N:M; el panel no la muestra aquí
        row.put("translatedName", translation != null ? translation.getTranslatedName() : null);
        row.put("translatedDescription", translation != null ? translation.getTranslatedDescription() : null);
        row.put("translationStatus", translation != null ? translation.getStatus().name() : null);
        row.put("hasTranslation", translation != null);
        row.put("needsReview", needsReview);
        return row;
    }
}
