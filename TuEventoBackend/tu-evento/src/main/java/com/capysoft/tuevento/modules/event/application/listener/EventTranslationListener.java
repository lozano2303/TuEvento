package com.capysoft.tuevento.modules.event.application.listener;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.event.domain.event.EventUpdatedEvent;
import com.capysoft.tuevento.modules.event.domain.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Listener que invalida traducciones cuando se actualiza un evento.
 * 
 * Similar al ProfileTranslationListener, solo elimina traducciones cuando
 * el contenido traducible del evento cambia (nombre o descripción).
 * 
 * Mantiene las traducciones existentes para cambios que no afectan contenido
 * traducible como fechas, capacidad, estado, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventTranslationListener {

    private final EventTranslationRepository eventTranslationRepository;
    private final TranslationRequests translationRequests;
    private final EventLogRepository eventLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventUpdated(EventUpdatedEvent event) {
        try {
            log.debug("[EventTranslationListener] Event {} updated, checking if content changed", 
                    event.getEventId());
            
            // Verificar si el contenido traducible cambió
            boolean contentChanged = checkIfTranslatableContentChanged(event.getEventId(), event.getOccurredAt());
            
            if (contentChanged) {
                log.info("[EventTranslationListener] Translatable content changed for event {}, invalidating translations", 
                        event.getEventId());
                
                // Eliminar todas las traducciones existentes
                eventTranslationRepository.deleteByEvent(event.getEventId().intValue());
                
                // Eliminar todos los jobs pendientes
                int deletedJobs = translationRequests.discard("event", event.getEventId());
                
                log.info("[EventTranslationListener] Event {} content changed - deleted translations and {} pending jobs. New translations will be generated on-demand.",
                        event.getEventId(), deletedJobs);
            } else {
                log.debug("[EventTranslationListener] Event {} updated but translatable content didn't change, keeping existing translations", 
                        event.getEventId());
            }
            
        } catch (Exception e) {
            log.error("[EventTranslationListener] Failed to process event update for event {}: {}", 
                    event.getEventId(), e.getMessage(), e);
            // Non-blocking: el evento ya fue actualizado exitosamente
        }
    }
    
    /**
     * Verifica si el contenido traducible cambió revisando los logs recientes del evento.
     * Busca entradas de EventLog con actions relacionadas con contenido traducible.
     */
    private boolean checkIfTranslatableContentChanged(Long eventId, LocalDateTime occurredAt) {
        try {
            // Buscar logs de los últimos 5 segundos para ver si hay cambios en contenido traducible
            LocalDateTime startTime = occurredAt.minusSeconds(5);
            LocalDateTime endTime = occurredAt.plusSeconds(5);
            
            // Verificar cambios en nombre o descripción del evento
            return eventLogRepository.existsByEventIdAndActionInAndOccurredAtBetween(
                    eventId, 
                    java.util.List.of("EVENT_NAME_CHANGED", "DESCRIPTION_CHANGED"), 
                    startTime, 
                    endTime);
                    
        } catch (Exception e) {
            log.warn("[EventTranslationListener] Error checking content changes for event {}, assuming content changed for safety: {}", 
                    eventId, e.getMessage());
            // En caso de error, asumir que cambió por seguridad
            return true;
        }
    }
}