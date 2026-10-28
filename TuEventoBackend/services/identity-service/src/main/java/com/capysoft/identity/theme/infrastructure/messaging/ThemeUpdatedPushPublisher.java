package com.capysoft.identity.theme.infrastructure.messaging;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.identity.theme.domain.event.ThemeActivatedEvent;
import com.capysoft.identity.theme.domain.event.ThemeCustomizationResetEvent;
import com.capysoft.identity.theme.domain.event.ThemeCustomizedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Avisa en vivo a todas las sesiones del usuario (web y móvil) que su tema cambió, para que
 * vuelvan a pedir la paleta con GET /themes/my-active. El /ws sigue en el monolito, así que el
 * aviso va por {@link #TOPIC} y el monolito lo reenvía al canal {@code /topic/users/{userId}/session}
 * con el mismo payload de siempre: {@code { type: "THEME_UPDATED" }}.
 *
 * <p>Un aviso perdido solo retrasa el refresco: la paleta se lee igual al recargar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeUpdatedPushPublisher {

    public static final String TOPIC = "tuevento.identidad.push";
    static final String THEME_UPDATED = "THEME_UPDATED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onThemeActivated(ThemeActivatedEvent event) {
        push(event.getUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onThemeCustomized(ThemeCustomizedEvent event) {
        push(event.getUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCustomizationReset(ThemeCustomizationResetEvent event) {
        push(event.getUserId());
    }

    private void push(Integer userId) {
        if (userId == null) {
            return;
        }
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "destination", "/topic/users/" + userId + "/session",
                    "body", Map.of("type", THEME_UPDATED)));
            kafkaTemplate.send(TOPIC, String.valueOf(userId), message)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("[ThemeUpdated] push not sent for userId={}: {}", userId, error.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("[ThemeUpdated] push not sent for userId={}: {}", userId, e.getMessage());
        }
    }
}
