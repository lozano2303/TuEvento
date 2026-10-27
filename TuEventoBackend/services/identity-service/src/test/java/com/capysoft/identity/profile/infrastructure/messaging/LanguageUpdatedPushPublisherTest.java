package com.capysoft.identity.profile.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import com.capysoft.identity.profile.domain.event.ProfileUpdatedEvent;
import com.capysoft.identity.theme.infrastructure.messaging.ThemeUpdatedPushPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("LanguageUpdatedPushPublisher")
class LanguageUpdatedPushPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final LanguageUpdatedPushPublisher publisher = new LanguageUpdatedPushPublisher(kafka, objectMapper);

    @Test
    @DisplayName("cambio de idioma → LANGUAGE_UPDATED con el languageId al canal de sesión")
    void notifiesLanguageChange() throws Exception {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());

        publisher.onProfileUpdated(ProfileUpdatedEvent.builder().profileId(30L).userId(7).changedLanguageId(2L).build());

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(ThemeUpdatedPushPublisher.TOPIC), eq("7"), sent.capture());
        assertThat(objectMapper.readValue(sent.getValue(), new TypeReference<Map<String, Object>>() { }))
                .containsEntry("destination", "/topic/users/7/session")
                .containsEntry("body", Map.of("type", "LANGUAGE_UPDATED", "languageId", 2));
    }

    @Test
    @DisplayName("sin cambio de idioma no publica; Kafka caído no rompe la petición")
    void otherUpdatesAndFailures() {
        publisher.onProfileUpdated(ProfileUpdatedEvent.builder().profileId(30L).userId(7).build());
        verifyNoInteractions(kafka);

        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("broker down"));
        assertThatNoException().isThrownBy(() -> publisher.onProfileUpdated(
                ProfileUpdatedEvent.builder().profileId(30L).userId(7).changedLanguageId(2L).build()));
    }
}
