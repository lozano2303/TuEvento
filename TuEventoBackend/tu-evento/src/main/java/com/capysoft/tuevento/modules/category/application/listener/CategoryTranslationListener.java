package com.capysoft.tuevento.modules.category.application.listener;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.category.domain.event.CategoryCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener para eventos de categorías que solicita traducciones automáticas.
 * Usa @TransactionalEventListener para ejecutar DESPUÉS de que la transacción se complete.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryTranslationListener {

    private final TranslationRequests translationRequests;

    /**
     * Solicita traducciones automáticas cuando se crea una categoría.
     * Se ejecuta DESPUÉS de que la transacción se complete para evitar race conditions.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(CategoryCreatedEvent event) {
        try {
            log.debug("Requesting automatic translations for newly created category: id={}", event.getCategoryId());
            translationRequests.request("category", event.getCategoryId().longValue());
            log.info("Translation jobs created for category: id={}, name={}", event.getCategoryId(), event.getName());
        } catch (Exception e) {
            log.warn("Failed to request translations for category {}: {}", event.getCategoryId(), e.getMessage());
            // Non-blocking: translation failure should not affect category creation
        }
    }
}
