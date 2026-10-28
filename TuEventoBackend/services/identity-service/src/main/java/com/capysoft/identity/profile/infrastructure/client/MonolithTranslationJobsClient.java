package com.capysoft.identity.profile.infrastructure.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.identity.profile.application.port.out.TranslationJobsPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Trabajos de traducción de la biografía en el motor de traducciones, que hoy vive en el monolito.
 * <ul>
 *   <li>Estado (lectura): {@code GET /internal/profiles/{id}/translation-jobs/{languageId}} con la
 *       clave {@code X-Internal-Api-Key}.</li>
 *   <li>Pedir o descartar (escritura): un mensaje en {@link #TOPIC} con el profileId como clave, así
 *       el descarte por un cambio de biografía llega antes que la petición siguiente.</li>
 * </ul>
 */
@Slf4j
@Component
public class MonolithTranslationJobsClient implements TranslationJobsPort {

    public static final String TOPIC = "tuevento.identidad.translation";
    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public MonolithTranslationJobsClient(RestClient.Builder builder,
                                         @Value("${app.monolith.base-url:http://localhost:8080}") String baseUrl,
                                         @Value("${app.internal.api-key:}") String apiKey,
                                         KafkaTemplate<String, String> kafkaTemplate,
                                         ObjectMapper objectMapper) {
        this(builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(timeouts()).build(),
                kafkaTemplate, objectMapper);
    }

    MonolithTranslationJobsClient(RestClient restClient, KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobState stateOf(Long profileId, Integer targetLanguageId) {
        try {
            Envelope body = restClient.get()
                    .uri("/internal/profiles/{profileId}/translation-jobs/{languageId}", profileId, targetLanguageId)
                    .retrieve().body(Envelope.class);
            if (body == null || body.data() == null || body.data().state() == null) {
                log.warn("Translation job state missing for profile {} -> {}", profileId, targetLanguageId);
                return JobState.FAILED;
            }
            return JobState.valueOf(body.data().state());
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Translation job state unavailable for profile {} -> {}: {}", profileId, targetLanguageId,
                    e.getMessage());
            return JobState.FAILED;
        }
    }

    @Override
    public void request(Long profileId, String sourceLanguageCode) {
        send(profileId, Map.of("action", "REQUEST", "profileId", profileId, "sourceLanguage", sourceLanguageCode));
    }

    @Override
    public void discard(Long profileId) {
        send(profileId, Map.of("action", "DISCARD", "profileId", profileId));
    }

    private void send(Long profileId, Map<String, Object> message) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(profileId), objectMapper.writeValueAsString(message))
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Translation command {} not sent for profile {}: {}",
                                    message.get("action"), profileId, error.getMessage());
                        }
                    });
        } catch (Exception e) {
            // Sin traducción el perfil se muestra en español; se vuelve a pedir en la próxima lectura
            log.warn("Translation command {} not sent for profile {}: {}", message.get("action"), profileId,
                    e.getMessage());
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(StateData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StateData(String state) {
    }
}
