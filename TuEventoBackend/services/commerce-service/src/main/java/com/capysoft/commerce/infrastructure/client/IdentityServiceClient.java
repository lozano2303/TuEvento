package com.capysoft.commerce.infrastructure.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.commerce.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/** API interna de identity-service: compradores, destinatarios y usuarios del crédito masivo. */
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

    /** Datos públicos de un usuario (nombre, email, idioma preferido). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserSummary(Integer userId, String alias, String email, String fullName, Long preferredLanguageId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data) {
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

    private <T> T get(String path, ParameterizedTypeReference<Envelope<T>> type) {
        try {
            Envelope<T> body = restClient.get().uri(path).retrieve().body(type);
            if (body == null || body.data() == null) {
                throw new ServiceUnavailableException("IDENTITY_UNAVAILABLE", "Identity returned no data for " + path);
            }
            return body.data();
        } catch (RestClientException e) {
            log.warn("Identity service unavailable ({}): {}", path, e.getMessage());
            throw new ServiceUnavailableException("IDENTITY_UNAVAILABLE",
                    "The identity service is not available, try again later");
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
