package com.capysoft.identity.profile.infrastructure.messaging;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.identity.profile.domain.event.ProfileUpdatedEvent;
import com.capysoft.identity.theme.infrastructure.messaging.ThemeUpdatedPushPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Avisa en vivo a todas las sesiones del usuario (web y móvil) que cambió su idioma preferido,
 * para que lo apliquen sin recargar. Va por el mismo topic que los avisos de tema y el monolito lo
 * reenvía al canal {@code /topic/users/{userId}/session} con el payload de siempre:
 * {@code { type: "LANGUAGE_UPDATED", languageId: 2 }}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LanguageUpdatedPushPublisher {

    static final String LANGUAGE_UPDATED = "LANGUAGE_UPDATED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        if (event.getChangedLanguageId() == null || event.getUserId() == null) {
            return;
        }
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "destination", "/topic/users/" + event.getUserId() + "/session",
                    "body", Map.of("type", LANGUAGE_UPDATED, "languageId", event.getChangedLanguageId())));
            kafkaTemplate.send(ThemeUpdatedPushPublisher.TOPIC, String.valueOf(event.getUserId()), message)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("[LanguageUpdated] push not sent for userId={}: {}", event.getUserId(),
                                    error.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("[LanguageUpdated] push not sent for userId={}: {}", event.getUserId(), e.getMessage());
        }
    }
}
