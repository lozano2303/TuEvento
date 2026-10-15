package com.capysoft.tuevento.modules.language.application.listener;

import com.capysoft.tuevento.modules.language.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener que invalida traducciones cuando se actualiza un perfil.
 * 
 * Cuando un perfil se actualiza (potencialmente su biografía), este listener:
 * 1. Elimina todas las traducciones existentes
 * 2. Elimina todos los jobs de traducción pendientes
 * 
 * Las nuevas traducciones se generarán on-demand cuando un usuario solicite 
 * el perfil en otro idioma (lazy loading).
 * 
 * Este enfoque simple garantiza que las traducciones siempre reflejen el contenido actual,
 * sin necesidad de tracking de cambios o hashing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTranslationListener {

    private final ProfileTranslationRepository profileTranslationRepository;
    private final TranslationJobRepository translationJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        try {
            log.debug("[ProfileTranslationListener] Profile {} updated, invalidating translations", 
                    event.getProfileId());
            
            // Eliminar todas las traducciones existentes
            profileTranslationRepository.deleteByProfile(event.getProfileId());
            
            // Eliminar todos los jobs pendientes
            int deletedJobs = translationJobRepository.deleteByEntity("profile", event.getProfileId().intValue());
            
            log.info("[ProfileTranslationListener] Profile {} updated - deleted translations and {} pending jobs. New translations will be generated on-demand.",
                    event.getProfileId(), deletedJobs);
            
        } catch (Exception e) {
            log.error("[ProfileTranslationListener] Failed to invalidate translations for profile {}: {}", 
                    event.getProfileId(), e.getMessage(), e);
            // Non-blocking: el perfil ya fue actualizado exitosamente
        }
    }
}
