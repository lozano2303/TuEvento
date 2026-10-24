package com.capysoft.tuevento.modules.theme.infrastructure.websocket;

import com.capysoft.tuevento.modules.theme.domain.event.ThemeActivatedEvent;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeCustomizationResetEvent;
import com.capysoft.tuevento.modules.theme.domain.event.ThemeCustomizedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Avisa en vivo a todas las sesiones del usuario (web y móvil) que su tema cambió
 * (activación, personalización o reseteo), para que vuelvan a pedir la paleta
 * resuelta con GET /themes/my-active. Reemplaza el polling de 30 s.
 *
 * <p>Canal: {@code /topic/users/{userId}/session}
 * <p>Payload: {@code { type: "THEME_UPDATED" }}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeUpdatedWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onThemeActivated(ThemeActivatedEvent event) {
        notifyUser(event.getUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onThemeCustomized(ThemeCustomizedEvent event) {
        notifyUser(event.getUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCustomizationReset(ThemeCustomizationResetEvent event) {
        notifyUser(event.getUserId());
    }

    private void notifyUser(Integer userId) {
        if (userId == null) return;
        String topic = "/topic/users/" + userId + "/session";
        try {
            messagingTemplate.convertAndSend(topic, Map.of("type", "THEME_UPDATED"));
            log.debug("[ThemeUpdated] THEME_UPDATED sent to {}", topic);
        } catch (Exception e) {
            log.error("[ThemeUpdated] WebSocket push failed (non-breaking): userId={}, error={}",
                    userId, e.getMessage(), e);
        }
    }
}
