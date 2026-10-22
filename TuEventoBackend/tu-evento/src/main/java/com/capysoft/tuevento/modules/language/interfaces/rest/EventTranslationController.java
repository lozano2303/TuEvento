package com.capysoft.tuevento.modules.language.interfaces.rest;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.language.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
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
 * Controller para gestionar traducciones de eventos.
 * Permite a los administradores crear, actualizar y consultar traducciones manualmente.
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

    /**
     * Obtiene todos los eventos con sus traducciones para un idioma específico.
     * Útil para el panel de administración donde se gestionan traducciones.
     */
    @Operation(summary = "Get all events with translations for a language",
               description = "Lista todos los eventos del sistema con sus traducciones (si existen) para el idioma especificado. " +
                           "Puede filtrar para mostrar solo los que necesitan revisión.")
    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEventsWithTranslations(
            @Parameter(description = "Language code (en, fr, pt)", example = "en") 
            @PathVariable String languageCode,
            
            @Parameter(description = "Filter: all, needs-review, translated, not-translated", example = "all")
            @RequestParam(defaultValue = "all") String filter) {
        
        try {
            // Validar y obtener el idioma
            Optional<Language> languageOpt = languageRepository.findByCode(languageCode.toLowerCase());
            if (languageOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Language not found: " + languageCode));
            }
            
            Integer languageId = languageOpt.get().getLanguageId();
            
            // Obtener todos los eventos
            List<Event> events = eventRepository.findAll();
            
            // Construir respuesta con eventos y sus traducciones
            List<Map<String, Object>> result = new ArrayList<>();
            for (Event event : events) {
                Optional<EventTranslation> translationOpt = 
                        eventTranslationRepository.findByEventAndLanguage(event.getEventId().intValue(), languageId);
                
                // Aplicar filtro
                boolean includeEvent = false;
                boolean needsReview = false;
                
                if ("all".equals(filter)) {
                    includeEvent = true;
                } else if ("not-translated".equals(filter)) {
                    // Sin traducción o campos vacíos
                    includeEvent = translationOpt.isEmpty() || 
                                   translationOpt.get().getTranslatedName() == null || 
                                   translationOpt.get().getTranslatedName().trim().isEmpty();
                } else if ("translated".equals(filter)) {
                    // Tiene traducción completa
                    includeEvent = translationOpt.isPresent() && 
                                   translationOpt.get().getTranslatedName() != null && 
                                   !translationOpt.get().getTranslatedName().trim().isEmpty();
                } else if ("needs-review".equals(filter)) {
                    // Sin traducción O traducción idéntica/similar al original
                    if (translationOpt.isEmpty()) {
                        includeEvent = true;
                        needsReview = true;
                    } else {
                        EventTranslation trans = translationOpt.get();
                        String original = event.getEventName();
                        String translated = trans.getTranslatedName();
                        
                        // Necesita revisión si:
                        // 1. No tiene traducción
                        // 2. La traducción es idéntica al original
                        // 3. La traducción es muy similar (solo difiere en mayúsculas/espacios)
                        if (translated == null || translated.trim().isEmpty()) {
                            includeEvent = true;
                            needsReview = true;
                        } else if (original.equals(translated)) {
                            includeEvent = true;
                            needsReview = true;
                        } else if (original.toLowerCase().trim().equals(translated.toLowerCase().trim())) {
                            includeEvent = true;
                            needsReview = true;
                        }
                    }
                }
                
                if (!includeEvent) {
                    continue;
                }
                
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("eventId", event.getEventId());
                eventData.put("eventName", event.getEventName());
                eventData.put("description", event.getDescription() != null ? event.getDescription() : "");
                eventData.put("categoryName", ""); // Event no tiene categoryId directo, es relación N:M
                eventData.put("translatedName", translationOpt.map(EventTranslation::getTranslatedName).orElse(null));
                eventData.put("translatedDescription", translationOpt.map(EventTranslation::getTranslatedDescription).orElse(null));
                eventData.put("translationStatus", translationOpt.map(t -> t.getStatus().name()).orElse(null));
                eventData.put("hasTranslation", translationOpt.isPresent());
                eventData.put("needsReview", needsReview);
                
                result.add(eventData);
            }
            
            log.info("Retrieved {} events with translations for language {}", result.size(), languageCode);
            return ResponseEntity.ok(ApiResponse.ok("Events with translations retrieved", result));
            
        } catch (Exception e) {
            log.error("Error getting events with translations for language: {}", languageCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving events: " + e.getMessage()));
        }
    }

    /**
     * Crea o actualiza la traducción de un evento para un idioma específico.
     * Si la traducción ya existe, la actualiza. Si no existe, la crea.
     */
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
            // Validar que el evento existe
            Optional<Event> eventOpt = eventRepository.findById(eventId.longValue());
            if (eventOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Event not found with ID: " + eventId));
            }
            
            // Validar que el idioma existe
            Optional<Language> languageOpt = languageRepository.findById(languageId);
            if (languageOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Language not found with ID: " + languageId));
            }
            
            // Validar que no sea español (idioma fuente)
            if ("es".equalsIgnoreCase(languageOpt.get().getCode())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Cannot create translation for source language (Spanish)"));
            }
            
            // Buscar traducción existente o crear nueva
            EventTranslation translation = eventTranslationRepository
                    .findByEventAndLanguage(eventId, languageId)
                    .orElse(EventTranslation.builder()
                            .eventId(eventId)
                            .languageId(languageId)
                            .source(TranslationSource.MANUAL)
                            .status(TranslationStatus.DRAFT)
                            .build());
            
            // Actualizar campos
            String translatedName = translationData.get("translatedName");
            String translatedDescription = translationData.get("translatedDescription");
            
            if (translatedName != null) {
                translation.setTranslatedName(translatedName.trim());
            }
            if (translatedDescription != null) {
                translation.setTranslatedDescription(translatedDescription.trim());
            }
            
            // Si viene source en el request, usarlo (sino usar MANUAL por defecto)
            if (translationData.containsKey("source")) {
                try {
                    translation.setSource(TranslationSource.valueOf(translationData.get("source")));
                } catch (IllegalArgumentException e) {
                    translation.setSource(TranslationSource.MANUAL);
                }
            } else {
                translation.setSource(TranslationSource.MANUAL);
            }
            
            // Si viene status en el request, usarlo (sino usar PUBLISHED por defecto para admin)
            if (translationData.containsKey("status")) {
                try {
                    translation.setStatus(TranslationStatus.valueOf(translationData.get("status")));
                } catch (IllegalArgumentException e) {
                    translation.setStatus(TranslationStatus.PUBLISHED);
                }
            } else {
                translation.setStatus(TranslationStatus.PUBLISHED);
            }
            
            // Guardar
            EventTranslation saved = eventTranslationRepository.save(translation);
            
            log.info("Translation {} for event {} in language {} by admin", 
                    saved.getTranslationId() == null ? "created" : "updated", 
                    eventId, languageId);
            
            return ResponseEntity.ok(ApiResponse.ok("Translation saved successfully", saved));
            
        } catch (Exception e) {
            log.error("Error saving event translation for event {} in language {}", eventId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error saving translation: " + e.getMessage()));
        }
    }

    /**
     * Obtiene una traducción específica por evento y idioma.
     */
    @Operation(summary = "Get specific event translation",
               description = "Obtiene la traducción de un evento para un idioma específico")
    @GetMapping("/{eventId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<EventTranslation>> getTranslation(
            @Parameter(description = "Event ID") 
            @PathVariable Integer eventId,
            
            @Parameter(description = "Language ID") 
            @PathVariable Integer languageId) {
        
        try {
            Optional<EventTranslation> translationOpt = 
                    eventTranslationRepository.findByEventAndLanguage(eventId, languageId);
            
            if (translationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(ApiResponse.ok("Translation retrieved", translationOpt.get()));
            
        } catch (Exception e) {
            log.error("Error getting translation for event {} in language {}", eventId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * Elimina una traducción específica.
     */
    @Operation(summary = "Delete event translation",
               description = "Elimina la traducción de un evento para un idioma específico")
    @DeleteMapping("/{eventId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTranslation(
            @Parameter(description = "Event ID") 
            @PathVariable Integer eventId,
            
            @Parameter(description = "Language ID") 
            @PathVariable Integer languageId) {
        
        try {
            Optional<EventTranslation> translationOpt = 
                    eventTranslationRepository.findByEventAndLanguage(eventId, languageId);
            
            if (translationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            eventTranslationRepository.delete(translationOpt.get());
            
            log.info("Translation deleted for event {} in language {}", eventId, languageId);
            return ResponseEntity.ok(ApiResponse.ok("Translation deleted successfully", null));
            
        } catch (Exception e) {
            log.error("Error deleting translation for event {} in language {}", eventId, languageId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error deleting translation: " + e.getMessage()));
        }
    }

    /**
     * Obtiene todas las traducciones de un evento específico (todos los idiomas).
     */
    @Operation(summary = "Get all translations for an event",
               description = "Obtiene todas las traducciones de un evento en todos los idiomas")
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<EventTranslation>>> getAllTranslationsForEvent(
            @Parameter(description = "Event ID") 
            @PathVariable Integer eventId) {
        
        try {
            // Validar que el evento existe
            Optional<Event> eventOpt = eventRepository.findById(eventId.longValue());
            if (eventOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Event not found with ID: " + eventId));
            }
            
            List<EventTranslation> translations = eventTranslationRepository.findByEvent(eventId);
            
            return ResponseEntity.ok(ApiResponse.ok(
                    "Translations retrieved for event " + eventId, 
                    translations));
            
        } catch (Exception e) {
            log.error("Error getting all translations for event {}", eventId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }
}
