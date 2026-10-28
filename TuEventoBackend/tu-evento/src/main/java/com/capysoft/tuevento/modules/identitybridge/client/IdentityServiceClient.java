package com.capysoft.tuevento.modules.identitybridge.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente de la API interna de identity-service ({@code /internal/**}, con la clave entre
 * servicios): lo único del monolito que habla con Identidad. Si no responde, 503
 * ({@link ServiceUnavailableException}); nunca se confunde con un dato inexistente.
 */
@Slf4j
@Component
public class IdentityServiceClient {

    /** Igual que el servicio: nunca más de 500 IDs por petición. */
    static final int MAX_IDS_PER_REQUEST = 500;
    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    @Autowired
    public IdentityServiceClient(RestClient.Builder builder,
                                 @Value("${app.identity.base-url:http://localhost:8088}") String baseUrl,
                                 @Value("${app.internal.api-key:}") String apiKey) {
        this(builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(timeouts()).build());
    }

    IdentityServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Datos públicos de un usuario (nombre, email, foto, idioma preferido). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserSummary(Integer userId, String alias, String email, String fullName,
                              Integer storedFileId, Long preferredLanguageId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StatusData(TranslationStatus status) {
    }

    /** Resúmenes de los usuarios pedidos, en bloques de 500; los desconocidos no aparecen. */
    public Map<Integer, UserSummary> getUserSummaries(Collection<Integer> userIds) {
        List<Integer> ids = userIds.stream().filter(id -> id != null).distinct().toList();
        Map<Integer, UserSummary> found = new HashMap<>();
        for (int from = 0; from < ids.size(); from += MAX_IDS_PER_REQUEST) {
            List<Integer> chunk = ids.subList(from, Math.min(from + MAX_IDS_PER_REQUEST, ids.size()));
            String joined = String.join(",", chunk.stream().map(String::valueOf).toList());
            List<UserSummary> summaries = get("/internal/users/summary?ids=" + joined,
                    new ParameterizedTypeReference<Envelope<List<UserSummary>>>() { });
            summaries.forEach(s -> found.put(s.userId(), s));
        }
        return found;
    }

    public long countUsers() {
        return get("/internal/users/count", new ParameterizedTypeReference<Envelope<Long>>() { });
    }

    public List<Integer> findUserIdsAfter(int afterUserId, int limit) {
        return new ArrayList<>(get("/internal/users/ids?afterUserId=" + afterUserId + "&limit=" + limit,
                new ParameterizedTypeReference<Envelope<List<Integer>>>() { }));
    }

    public long pendingOrganizerRequests() {
        return get("/internal/admin/organizer-requests/pending-count", new ParameterizedTypeReference<Envelope<Long>>() { });
    }

    public Map<String, String> profileTranslatableTexts(Long profileId) {
        return get("/internal/profiles/" + profileId + "/translatable-texts",
                new ParameterizedTypeReference<Envelope<Map<String, String>>>() { });
    }

    public void saveProfileTranslation(Long profileId, String languageCode, Map<String, String> texts,
                                       TranslationSource source, TranslationStatus status) {
        try {
            restClient.put().uri("/internal/profiles/{id}/translations/{lang}", profileId, languageCode)
                    .body(Map.of("texts", texts, "source", source, "status", status))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw unavailable("save profile translation", e);
        }
    }

    public Optional<TranslationStatus> profileTranslationStatus(Long profileId, String languageCode) {
        try {
            Envelope<StatusData> body = restClient.get()
                    .uri("/internal/profiles/{id}/translations/{lang}/status", profileId, languageCode)
                    .retrieve().body(new ParameterizedTypeReference<Envelope<StatusData>>() { });
            return Optional.ofNullable(body).map(Envelope::data).map(StatusData::status);
        } catch (RestClientException e) {
            throw unavailable("profile translation status", e);
        }
    }

    /** Asigna el avatar por defecto a los perfiles que no tienen; devuelve cuántos cambió. */
    public int assignDefaultAvatar(Integer storedFileId) {
        try {
            Envelope<Integer> body = restClient.put().uri("/internal/profiles/default-avatar?storedFileId={id}", storedFileId)
                    .retrieve().body(new ParameterizedTypeReference<Envelope<Integer>>() { });
            return body == null || body.data() == null ? 0 : body.data();
        } catch (RestClientException e) {
            throw unavailable("assign default avatar", e);
        }
    }

    private <T> T get(String path, ParameterizedTypeReference<Envelope<T>> type) {
        try {
            Envelope<T> body = restClient.get().uri(path).retrieve().body(type);
            if (body == null || body.data() == null) {
                throw new ServiceUnavailableException("IDENTITY_UNAVAILABLE", "Identity returned no data for " + path);
            }
            return body.data();
        } catch (RestClientException e) {
            throw unavailable(path, e);
        }
    }

    private static ServiceUnavailableException unavailable(String what, Exception e) {
        log.warn("Identity service unavailable ({}): {}", what, e.getMessage());
        return new ServiceUnavailableException("IDENTITY_UNAVAILABLE",
                "The identity service is not available, try again later");
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
