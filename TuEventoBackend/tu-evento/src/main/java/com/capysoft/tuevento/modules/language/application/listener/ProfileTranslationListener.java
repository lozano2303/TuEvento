package com.capysoft.tuevento.modules.language.application.listener;

import com.capysoft.tuevento.modules.language.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Listener que invalida traducciones cuando se actualiza un perfil.
 * 
 * Cuando un perfil se actualiza, este listener:
 * 1. Verifica si realmente cambió el contenido traducible (bio)
 * 2. Solo elimina traducciones si el bio cambió
 * 3. Mantiene traducciones existentes para cambios que no afectan contenido traducible
 * 
 * Las nuevas traducciones se generarán on-demand cuando un usuario solicite 
 * el perfil en otro idioma (lazy loading).
 * 
 * Este enfoque optimizado evita traducciones innecesarias cuando solo cambia
 * la preferencia de idioma u otros campos que no requieren traducción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTranslationListener {

    private final ProfileTranslationRepository profileTranslationRepository;
    private final TranslationJobRepository translationJobRepository;
    private final ProfileLogRepository profileLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        try {
            log.debug("[ProfileTranslationListener] Profile {} updated, checking if bio changed", 
                    event.getProfileId());
            
            // Verificar si el bio cambió en esta actualización
            boolean bioChanged = checkIfBioChanged(event.getProfileId(), event.getOccurredAt());
            
            if (bioChanged) {
                log.info("[ProfileTranslationListener] Bio changed for profile {}, invalidating translations", 
                        event.getProfileId());
                
                // Eliminar todas las traducciones existentes
                profileTranslationRepository.deleteByProfile(event.getProfileId());
                
                // Eliminar todos los jobs pendientes
                int deletedJobs = translationJobRepository.deleteByEntity("profile", event.getProfileId().intValue());
                
                log.info("[ProfileTranslationListener] Profile {} bio changed - deleted translations and {} pending jobs. New translations will be generated on-demand.",
                        event.getProfileId(), deletedJobs);
            } else {
                log.debug("[ProfileTranslationListener] Profile {} updated but bio didn't change, keeping existing translations", 
                        event.getProfileId());
            }
            
        } catch (Exception e) {
            log.error("[ProfileTranslationListener] Failed to process profile update for profile {}: {}", 
                    event.getProfileId(), e.getMessage(), e);
            // Non-blocking: el perfil ya fue actualizado exitosamente
        }
    }
    
    /**
     * Verifica si el bio cambió revisando los logs recientes del perfil.
     * Busca entradas de ProfileLog con action "BIO_CHANGED" cerca del tiempo del evento.
     */
    private boolean checkIfBioChanged(Long profileId, LocalDateTime occurredAt) {
        try {
            // Buscar logs de los últimos 5 segundos para ver si hay cambio de bio
            LocalDateTime startTime = occurredAt.minusSeconds(5);
            LocalDateTime endTime = occurredAt.plusSeconds(5);
            
            return profileLogRepository.existsByProfileIdAndActionAndOccurredAtBetween(
                    profileId, "BIO_CHANGED", startTime, endTime);
                    
        } catch (Exception e) {
            log.warn("[ProfileTranslationListener] Error checking bio changes for profile {}, assuming bio changed for safety: {}", 
                    profileId, e.getMessage());
            // En caso de error, asumir que cambió por seguridad
            return true;
        }
    }
}
