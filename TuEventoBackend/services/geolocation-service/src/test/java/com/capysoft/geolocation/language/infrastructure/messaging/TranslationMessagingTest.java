package com.capysoft.geolocation.language.infrastructure.messaging;

import com.capysoft.geolocation.language.application.service.TranslationService;
import com.capysoft.geolocation.language.domain.repository.TranslationJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Órdenes que llegan del monolito y resultados que vuelven a él. */
@DisplayName("Mensajería de traducciones")
class TranslationMessagingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("TranslationCommandListener")
    class Commands {

        private final TranslationService service = mock(TranslationService.class);
        private final TranslationJobRepository jobs = mock(TranslationJobRepository.class);
        private final TranslationCommandListener listener = new TranslationCommandListener(service, jobs, objectMapper);

        private String json(TranslationCommandMessage message) throws Exception {
            return objectMapper.writeValueAsString(message);
        }

        @Test
        @DisplayName("una petición crea los trabajos con el texto recibido")
        void request() throws Exception {
            listener.onCommand(json(new TranslationCommandMessage(TranslationCommandMessage.REQUEST, "event", 7L,
                    "es", Map.of("event_name", "Concierto"))));

            verify(service).requestTranslation("event", 7L, "es", Map.of("event_name", "Concierto"));
        }

        @Test
        @DisplayName("un descarte borra los trabajos de la entidad")
        void discard() throws Exception {
            listener.onCommand(json(new TranslationCommandMessage(TranslationCommandMessage.DISCARD, "profile", 9L,
                    null, null)));

            verify(jobs).deleteByEntity("profile", 9);
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("mensajes ilegibles, sin entidad, sin texto o con acción desconocida se descartan")
        void discardsInvalid() throws Exception {
            listener.onCommand("{no es json");
            listener.onCommand(json(new TranslationCommandMessage(TranslationCommandMessage.REQUEST, null, 7L, "es", Map.of())));
            listener.onCommand(json(new TranslationCommandMessage(TranslationCommandMessage.REQUEST, "event", 7L, "es", null)));
            listener.onCommand(json(new TranslationCommandMessage("RENAME", "event", 7L, "es", Map.of())));

            verifyNoInteractions(service);
            verify(jobs, org.mockito.Mockito.never()).deleteByEntity(anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("KafkaTranslationResults")
    class Results {

        @SuppressWarnings("unchecked")
        private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        private final KafkaTranslationResults results = new KafkaTranslationResults(kafka, objectMapper);

        @Test
        @DisplayName("publica la traducción con la clave de la entidad")
        void publishes() throws Exception {
            when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

            results.deliver("event", 7L, "en", Map.of("event_name", "Concert"));

            org.mockito.ArgumentCaptor<String> payload = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(kafka).send(org.mockito.ArgumentMatchers.eq(TranslationTopics.RESULTS),
                    org.mockito.ArgumentMatchers.eq("event:7"), payload.capture());
            assertThat(objectMapper.readValue(payload.getValue(), TranslationResultMessage.class))
                    .isEqualTo(new TranslationResultMessage("event", 7L, "en", Map.of("event_name", "Concert")));
        }

        @Test
        @DisplayName("si Kafka falla, el error llega al procesador para reintentar el trabajo")
        void failurePropagates() {
            when(kafka.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

            assertThatThrownBy(() -> results.deliver("event", 7L, "en", Map.of()))
                    .isInstanceOf(CompletionException.class);
            verify(kafka).send(anyString(), anyString(), any());
        }
    }
}
