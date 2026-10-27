package com.capysoft.tuevento.shared.infrastructure.translation;

import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Traducciones en modo remote: órdenes al servicio de localización, resultados de vuelta y catálogo de idiomas. */
@DisplayName("Traducción remota")
class RemoteTranslationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TranslatableContents contents = mock(TranslatableContents.class);

    @Nested
    @DisplayName("KafkaTranslationRequests")
    class Requests {

        @SuppressWarnings("unchecked")
        private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        private final KafkaTranslationRequests requests = new KafkaTranslationRequests(kafka, objectMapper, jdbc, contents);

        private JsonNode published() throws Exception {
            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(kafka).send(eq(TranslationTopics.COMMANDS), eq("event:7"), payload.capture());
            return objectMapper.readTree(payload.getValue());
        }

        @Test
        @DisplayName("una petición viaja con el texto original y el idioma origen")
        void request() throws Exception {
            when(contents.sourceTexts("event", 7L)).thenReturn(Map.of("event_name", "Concierto"));

            requests.request("event", 7L);

            JsonNode command = published();
            assertThat(command.get("action").asText()).isEqualTo("REQUEST");
            assertThat(command.get("entityType").asText()).isEqualTo("event");
            assertThat(command.get("entityId").asLong()).isEqualTo(7L);
            assertThat(command.get("sourceLanguage").asText()).isEqualTo("es");
            assertThat(command.get("texts").get("event_name").asText()).isEqualTo("Concierto");
        }

        @Test
        @DisplayName("sin texto que traducir no publica nada")
        void nothingToTranslate() {
            when(contents.sourceTexts("event", 7L)).thenReturn(Map.of());

            requests.request("event", 7L);

            verifyNoInteractions(kafka);
        }

        @Test
        @DisplayName("un descarte se publica en el mismo orden que las peticiones de la entidad")
        void discard() throws Exception {
            assertThat(requests.discard("event", 7L)).isZero();

            assertThat(published().get("action").asText()).isEqualTo("DISCARD");
        }

        @Test
        @DisplayName("si Kafka falla no rompe el flujo del dominio")
        void publishFailureIsLogged() {
            when(contents.sourceTexts("event", 7L)).thenReturn(Map.of("event_name", "Concierto"));
            when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("broker caído"));

            assertThatCode(() -> requests.request("event", 7L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("el estado del trabajo se lee de translation_job; bajo demanda pide si no hay trabajo")
        void jobStatus() {
            when(jdbc.queryForList(anyString(), eq(String.class), eq("event"), eq(7L), eq(2)))
                    .thenReturn(List.of("PROCESSING"));
            when(jdbc.queryForList(anyString(), eq(String.class), eq("event"), eq(8L), eq(2))).thenReturn(List.of());
            when(contents.sourceTexts("event", 8L)).thenReturn(Map.of("event_name", "Feria"));

            assertThat(requests.jobStatus("event", 7L, 2)).contains(TranslationJobStatus.PROCESSING);
            assertThat(requests.ensureRequested("event", 7L, 2)).isTrue();
            assertThat(requests.ensureRequested("event", 8L, 2)).isTrue();
            verify(kafka).send(eq(TranslationTopics.COMMANDS), eq("event:8"), anyString());
            verify(kafka, never()).send(eq(TranslationTopics.COMMANDS), eq("event:7"), anyString());
        }
    }

    @Nested
    @DisplayName("TranslationResultListener")
    class Results {

        private final TranslationResultListener listener = new TranslationResultListener(contents, objectMapper);

        @Test
        @DisplayName("guarda la traducción recibida en el dominio")
        void saves() throws Exception {
            listener.onResult(objectMapper.writeValueAsString(Map.of(
                    "entityType", "profile", "entityId", 9, "languageCode", "pt", "texts", Map.of("bio", "Olá"))));

            verify(contents).saveMachineTranslation("profile", 9L, "pt", Map.of("bio", "Olá"));
        }

        @Test
        @DisplayName("mensajes ilegibles o incompletos se descartan")
        void discardsInvalid() throws Exception {
            listener.onResult("{no es json");
            listener.onResult(objectMapper.writeValueAsString(Map.of("entityType", "profile", "entityId", 9)));

            verifyNoInteractions(contents);
        }
    }

    @Nested
    @DisplayName("JdbcLanguageCatalog")
    class Catalog {

        private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
        private final JdbcLanguageCatalog catalog = new JdbcLanguageCatalog(jdbc);

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("busca por código y por id y devuelve id y código")
        void finds() throws Exception {
            LanguageSummary english = LanguageSummary.builder().languageId(2).code("en").build();
            when(jdbc.query(anyString(), any(RowMapper.class), eq("en"))).thenReturn(List.of(english));
            when(jdbc.query(anyString(), any(RowMapper.class), eq(9))).thenReturn(List.of());

            assertThat(catalog.findByCode("en")).contains(english);
            assertThat(catalog.findById(9)).isEmpty();

            ArgumentCaptor<RowMapper<LanguageSummary>> mapper = ArgumentCaptor.forClass(RowMapper.class);
            verify(jdbc).query(anyString(), mapper.capture(), eq("en"));
            ResultSet row = mock(ResultSet.class);
            when(row.getInt("language_id")).thenReturn(3);
            when(row.getString("code")).thenReturn("fr");
            assertThat(mapper.getValue().mapRow(row, 0)).isEqualTo(LanguageSummary.builder().languageId(3).code("fr").build());
        }
    }
}
