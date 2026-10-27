package com.capysoft.tuevento.modules.section.application.listener;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.section.domain.event.SectionTypeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener para eventos de tipos de sección que solicita traducciones automáticas.
 * Usa @TransactionalEventListener para ejecutar DESPUÉS de que la transacción se complete.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SectionTypeTranslationListener {

    private final TranslationRequests translationRequests;

    /**
     * Solicita traducciones automáticas cuando se crea un tipo de sección.
     * Se ejecuta DESPUÉS de que la transacción se complete para evitar race conditions.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSectionTypeCreated(SectionTypeCreatedEvent event) {
        try {
            log.debug("Requesting automatic translations for newly created section type: id={}", event.getSectionTypeId());
            translationRequests.request("section_type", event.getSectionTypeId().longValue());
            log.info("Translation jobs created for section type: id={}, name={}", event.getSectionTypeId(), event.getName());
        } catch (Exception e) {
            log.warn("Failed to request translations for section type {}: {}", event.getSectionTypeId(), e.getMessage());
            // Non-blocking: translation failure should not affect section type creation
        }
    }
}
