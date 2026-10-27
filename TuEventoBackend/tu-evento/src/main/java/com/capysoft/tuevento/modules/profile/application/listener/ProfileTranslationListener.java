package com.capysoft.tuevento.modules.profile.application.listener;

import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Invalida las traducciones de un perfil cuando cambia su biografía: se eliminan sus traducciones
 * y los trabajos pendientes, y las nuevas se generan bajo demanda. Otros cambios del perfil (idioma
 * preferido, nombre…) conservan las traducciones. Es best-effort: el perfil ya se guardó y un fallo
 * aquí no se propaga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTranslationListener {

    /** Margen alrededor del evento para encontrar el registro BIO_CHANGED de la misma edición. */
    private static final long BIO_LOG_WINDOW_SECONDS = 5;

    private final ProfileTranslationRepository profileTranslationRepository;
    private final TranslationJobRepository translationJobRepository;
    private final ProfileLogRepository profileLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        Long profileId = event.getProfileId();
        try {
            if (!bioChanged(profileId, event.getOccurredAt())) {
                log.debug("[ProfileTranslationListener] Profile {} updated without bio change, keeping translations",
                        profileId);
                return;
            }
            profileTranslationRepository.deleteByProfile(profileId);
            int deletedJobs = translationJobRepository.deleteByEntity("profile", profileId.intValue());
            log.info("[ProfileTranslationListener] Profile {} bio changed - deleted its translations and {} pending jobs",
                    profileId, deletedJobs);
        } catch (Exception e) {
            log.error("[ProfileTranslationListener] Failed to invalidate translations of profile {}: {}",
                    profileId, e.getMessage(), e);
        }
    }

    /** Si no se puede comprobar, se asume que cambió: es preferible regenerar a mostrar una traducción vieja. */
    private boolean bioChanged(Long profileId, LocalDateTime occurredAt) {
        try {
            return profileLogRepository.existsByProfileIdAndActionAndOccurredAtBetween(profileId, "BIO_CHANGED",
                    occurredAt.minusSeconds(BIO_LOG_WINDOW_SECONDS), occurredAt.plusSeconds(BIO_LOG_WINDOW_SECONDS));
        } catch (Exception e) {
            log.warn("[ProfileTranslationListener] Could not check bio changes of profile {}, assuming it changed: {}",
                    profileId, e.getMessage());
            return true;
        }
    }
}
