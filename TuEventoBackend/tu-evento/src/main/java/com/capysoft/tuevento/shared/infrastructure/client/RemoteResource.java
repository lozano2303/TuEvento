package com.capysoft.tuevento.shared.infrastructure.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Lectura por id de un recurso que otro servicio expone como {@code GET <path>/{id}} envuelto en
 * {@code ApiResponse}. Es la pieza común de los clientes REST entre servicios:
 * <ul>
 *   <li>404 → {@link NotFoundException} con el código del recurso (el dato no existe);</li>
 *   <li>cualquier otro fallo o una respuesta sin {@code data} → {@link ServiceUnavailableException}
 *       (503), para no confundir un servicio caído con un dato inexistente;</li>
 *   <li>las respuestas válidas se guardan en memoria durante el TTL; los errores no.</li>
 * </ul>
 *
 * @param <T> forma del campo {@code data} que interesa al cliente
 */
@Slf4j
public final class RemoteResource<T> {

    private final RestClient restClient;
    private final String path;
    private final ParameterizedTypeReference<Envelope<T>> type;
    private final Errors errors;
    private final long ttlNanos;
    private final Map<Integer, Cached<T>> cache = new ConcurrentHashMap<>();

    /**
     * Códigos y textos de error del recurso.
     *
     * @param notFoundCode    código de negocio cuando no existe, p. ej. {@code SITE_NOT_FOUND}
     * @param resourceName    nombre para el mensaje, p. ej. {@code Site}
     * @param unavailableCode código cuando el servicio falla, p. ej. {@code GEOLOCATION_UNAVAILABLE}
     * @param serviceName     nombre del servicio para el mensaje, p. ej. {@code geolocation}
     */
    public record Errors(String notFoundCode, String resourceName,
                         String unavailableCode, String serviceName) {
    }

    /** Envoltura {@code ApiResponse}; solo interesa {@code data}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope<T>(T data) {
    }

    private record Cached<T>(T value, long expiresAt) {
    }

    /**
     * @param path ruta con un único marcador para el id, p. ej. {@code /api/v1/geolocation/sites/{id}}
     * @param type tipo concreto de la envoltura, p. ej. {@code new ParameterizedTypeReference<Envelope<SiteData>>() {}}
     */
    public RemoteResource(RestClient restClient, String path,
                          ParameterizedTypeReference<Envelope<T>> type, Errors errors, Duration ttl) {
        this.restClient = restClient;
        this.path = path;
        this.type = type;
        this.errors = errors;
        this.ttlNanos = ttl.toNanos();
    }

    /** {@link RestClient} con timeouts cortos (2 s de conexión, 5 s de lectura) hacia {@code baseUrl}. */
    public static RestClient restClient(RestClient.Builder builder, String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    public T get(Integer id) {
        Cached<T> cached = cache.get(id);
        if (cached != null && System.nanoTime() < cached.expiresAt()) {
            return cached.value();
        }
        T value = fetch(id);
        cache.put(id, new Cached<>(value, System.nanoTime() + ttlNanos));
        return value;
    }

    private T fetch(Integer id) {
        Envelope<T> body;
        try {
            body = restClient.get().uri(path, id).retrieve().body(type);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException(errors.notFoundCode(),
                    errors.resourceName() + " not found with id: " + id);
        } catch (RestClientException e) {
            log.warn("{} service failed for {} {}: {}", errors.serviceName(), errors.resourceName(), id, e.getMessage());
            throw unavailable();
        }
        if (body == null || body.data() == null) {
            log.warn("{} service returned no data for {} {}", errors.serviceName(), errors.resourceName(), id);
            throw unavailable();
        }
        return body.data();
    }

    private ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException(errors.unavailableCode(),
                "The " + errors.serviceName() + " service is not available, try again later");
    }
}
