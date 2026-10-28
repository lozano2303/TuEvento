package com.capysoft.identity.profile.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.identity.profile.application.port.out.TranslationJobsPort.JobState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("MonolithTranslationJobsClient (Identidad → motor de traducciones)")
class MonolithTranslationJobsClientTest {

    private static final String STATE_URL = "http://monolith/internal/profiles/30/translation-jobs/2";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://monolith")
            .defaultHeader(MonolithTranslationJobsClient.API_KEY_HEADER, "secret");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final MonolithTranslationJobsClient client =
            new MonolithTranslationJobsClient(builder.build(), kafka, objectMapper);

    @Test
    @DisplayName("lee el estado con la clave entre servicios")
    void readsState() {
        server.expect(requestTo(STATE_URL)).andExpect(header("X-Internal-Api-Key", "secret"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"state\":\"IN_PROGRESS\"}}", MediaType.APPLICATION_JSON));

        assertThat(client.stateOf(30L, 2)).isEqualTo(JobState.IN_PROGRESS);
        server.verify();
    }

    @Test
    @DisplayName("monolito caído, clave rechazada o respuesta rara → FAILED (el cliente deja de esperar)")
    void unavailableIsFailed() {
        server.expect(requestTo(STATE_URL)).andRespond(withServerError());
        server.expect(requestTo(STATE_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(STATE_URL))
                .andRespond(withSuccess("{\"data\":{\"state\":\"WHATEVER\"}}", MediaType.APPLICATION_JSON));

        assertThat(client.stateOf(30L, 2)).isEqualTo(JobState.FAILED);
        assertThat(client.stateOf(30L, 2)).isEqualTo(JobState.FAILED);
        assertThat(client.stateOf(30L, 2)).isEqualTo(JobState.FAILED);
        server.verify();
    }

    @Test
    @DisplayName("pedir y descartar van por Kafka con el profileId como clave")
    void commandsGoThroughKafka() throws Exception {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());

        client.request(30L, "es");
        client.discard(30L);

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(kafka, org.mockito.Mockito.times(2)).send(eq(MonolithTranslationJobsClient.TOPIC), eq("30"), sent.capture());
        TypeReference<Map<String, Object>> type = new TypeReference<>() { };
        assertThat(objectMapper.readValue(sent.getAllValues().get(0), type))
                .containsEntry("action", "REQUEST").containsEntry("profileId", 30).containsEntry("sourceLanguage", "es");
        assertThat(objectMapper.readValue(sent.getAllValues().get(1), type))
                .containsEntry("action", "DISCARD").containsEntry("profileId", 30);
    }
}
