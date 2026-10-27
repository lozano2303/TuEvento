package com.capysoft.identity.theme.infrastructure.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.identity.theme.application.port.out.LanguageDirectoryPort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Idiomas de Localización por su API pública ({@code GET /api/v1/languages}, idiomas activos), que
 * hoy sirve el monolito. Son datos de referencia: la lista se guarda en memoria durante el TTL.
 * Si Localización no responde, los temas se devuelven sin traducir (igual que con un idioma
 * desconocido) en vez de fallar la petición.
 */
@Slf4j
@Component
public class LocalizationLanguageClient implements LanguageDirectoryPort {

    private static final ParameterizedTypeReference<Envelope> TYPE = new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final long ttlNanos;
    private volatile Cached cached;

    @Autowired
    public LocalizationLanguageClient(RestClient.Builder builder,
                                      @Value("${app.localization.base-url:http://localhost:8080}") String baseUrl,
                                      @Value("${app.localization.cache-ttl:PT10M}") Duration cacheTtl) {
        this(builder.baseUrl(baseUrl).requestFactory(timeouts()).build(), cacheTtl);
    }

    LocalizationLanguageClient(RestClient restClient, Duration cacheTtl) {
        this.restClient = restClient;
        this.ttlNanos = cacheTtl.toNanos();
    }

    @Override
    public Optional<Integer> findIdByCode(String languageCode) {
        if (languageCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(languagesByCode().get(languageCode.toLowerCase()));
    }

    private Map<String, Integer> languagesByCode() {
        Cached current = cached;
        if (current != null && System.nanoTime() < current.expiresAt()) {
            return current.byCode();
        }
        try {
            Envelope body = restClient.get().uri("/api/v1/languages").retrieve().body(TYPE);
            if (body == null || body.data() == null) {
                log.warn("Localization returned no languages");
                return Map.of();
            }
            Map<String, Integer> byCode = body.data().stream()
                    .filter(l -> l.code() != null && l.languageId() != null)
                    .collect(Collectors.toUnmodifiableMap(l -> l.code().toLowerCase(), LanguageData::languageId,
                            (a, b) -> a));
            cached = new Cached(byCode, System.nanoTime() + ttlNanos);
            return byCode;
        } catch (RestClientException e) {
            // Sin caché nueva: el siguiente intento vuelve a preguntar
            log.warn("Localization languages unavailable: {}", e.getMessage());
            return current != null ? current.byCode() : Map.of();
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(List<LanguageData> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LanguageData(Integer languageId, String code) {
    }

    private record Cached(Map<String, Integer> byCode, long expiresAt) {
    }
}
