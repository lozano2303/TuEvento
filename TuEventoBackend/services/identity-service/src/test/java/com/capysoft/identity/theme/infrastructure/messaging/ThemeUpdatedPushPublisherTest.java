package com.capysoft.identity.theme.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

import com.capysoft.identity.theme.domain.event.ThemeActivatedEvent;
import com.capysoft.identity.theme.domain.event.ThemeCustomizationResetEvent;
import com.capysoft.identity.theme.domain.event.ThemeCustomizedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Mismo aviso que daba el monolito por WebSocket, ahora por Kafka para que el monolito lo reenvíe. */
@DisplayName("ThemeUpdatedPushPublisher")
class ThemeUpdatedPushPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final ThemeUpdatedPushPublisher publisher = new ThemeUpdatedPushPublisher(kafka, objectMapper);

    @Test
    @DisplayName("activar, personalizar o resetear → THEME_UPDATED al canal de sesión del usuario")
    void everyChangeNotifiesTheUserSession() throws Exception {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());

        publisher.onThemeActivated(ThemeActivatedEvent.builder().userId(7).themeId(1).build());
        publisher.onThemeCustomized(ThemeCustomizedEvent.builder().userId(7).property("primary").build());
        publisher.onCustomizationReset(ThemeCustomizationResetEvent.builder().userId(7).build());

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(kafka, org.mockito.Mockito.times(3)).send(eq(ThemeUpdatedPushPublisher.TOPIC), eq("7"), message.capture());
        Map<String, Object> sent = objectMapper.readValue(message.getValue(), new TypeReference<>() { });
        assertThat(sent).containsEntry("destination", "/topic/users/7/session")
                .containsEntry("body", Map.of("type", "THEME_UPDATED"));
    }

    @Test
    @DisplayName("sin userId no publica nada")
    void nullUserDoesNothing() {
        publisher.onThemeActivated(ThemeActivatedEvent.builder().build());

        verifyNoInteractions(kafka);
    }

    @Test
    @DisplayName("Kafka caído no rompe la petición que cambió el tema")
    void brokerFailureIsNonBreaking() {
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("broker down"));
        assertThatNoException().isThrownBy(() ->
                publisher.onThemeActivated(ThemeActivatedEvent.builder().userId(7).build()));

        doReturn(CompletableFuture.failedFuture(new RuntimeException("timeout")))
                .when(kafka).send(anyString(), anyString(), anyString());
        assertThatNoException().isThrownBy(() ->
                publisher.onThemeActivated(ThemeActivatedEvent.builder().userId(7).build()));
    }
}
