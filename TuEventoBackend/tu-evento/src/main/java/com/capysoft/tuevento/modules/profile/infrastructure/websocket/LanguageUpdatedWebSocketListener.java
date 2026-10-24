package com.capysoft.tuevento.modules.profile.infrastructure.websocket;

import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Avisa en vivo a todas las sesiones del usuario (web y móvil) que cambió su
 * idioma preferido, para que apliquen el nuevo idioma sin recargar.
 *
 * <p>Canal: {@code /topic/users/{userId}/session}
 * <p>Payload: {@code { type: "LANGUAGE_UPDATED", languageId: 2 }}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LanguageUpdatedWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        if (event.getChangedLanguageId() == null || event.getUserId() == null) return;
        String topic = "/topic/users/" + event.getUserId() + "/session";
        try {
            messagingTemplate.convertAndSend(topic,
                    Map.of("type", "LANGUAGE_UPDATED", "languageId", event.getChangedLanguageId()));
            log.debug("[LanguageUpdated] LANGUAGE_UPDATED sent to {}", topic);
        } catch (Exception e) {
            log.error("[LanguageUpdated] WebSocket push failed (non-breaking): userId={}, error={}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
