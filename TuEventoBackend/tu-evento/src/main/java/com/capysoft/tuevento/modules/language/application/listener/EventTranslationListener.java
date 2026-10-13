package com.capysoft.tuevento.modules.language.application.listener;

import com.capysoft.tuevento.modules.event.domain.event.EventCreatedEvent;
import com.capysoft.tuevento.modules.language.application.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener que solicita traducciones automáticamente cuando se crea un evento.
 * Se ejecuta DESPUÉS de que la transacción de creación se complete exitosamente,
 * evitando problemas de transacciones anidadas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventTranslationListener {

    private final TranslationService translationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCreated(EventCreatedEvent event) {
        try {
            log.debug("[EventTranslationListener] Event {} created, requesting translations", event.getEventId());
            translationService.requestTranslation("event", event.getEventId(), "es");
            log.info("[EventTranslationListener] Translation jobs created for event {}", event.getEventId());
        } catch (Exception e) {
            log.error("[EventTranslationListener] Failed to request translation for event {}: {}", 
                    event.getEventId(), e.getMessage(), e);
            // Non-blocking: el evento ya fue creado exitosamente
        }
    }
}
