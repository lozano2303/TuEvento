package com.capysoft.tuevento.modules.event.infrastructure.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.modules.event.application.port.out.SiteSummary;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Lee las sedes por la API pública de geolocalización ({@code GET /api/v1/geolocation/sites/{id}})
 * en vez de importar el módulo: así el evento no depende del código de localización. La URL sale
 * de {@code GEOLOCATION_SERVICE_URL} (el microservicio); sin ella apunta al propio monolito.
 *
 * <p>Las sedes casi no cambian y los listados piden la misma sede por cada evento, así que se
 * guardan en memoria durante {@code app.geolocation.cache-ttl}.
 */
@Slf4j
@Component
public class GeolocationSiteClient implements SiteLookupPort {

    private static final String SITE_PATH = "/api/v1/geolocation/sites/{siteId}";

    private final RestClient restClient;
    private final long cacheTtlNanos;
    private final Map<Integer, CachedSite> cache = new ConcurrentHashMap<>();

    @Autowired
    public GeolocationSiteClient(RestClient.Builder builder,
                                 @Value("${app.geolocation.base-url:${GEOLOCATION_SERVICE_URL:http://localhost:${server.port:8080}}}")
                                 String baseUrl,
                                 @Value("${app.geolocation.cache-ttl:PT1M}") Duration cacheTtl) {
        this(builder.baseUrl(baseUrl).requestFactory(requestFactory()).build(), cacheTtl);
    }

    GeolocationSiteClient(RestClient restClient, Duration cacheTtl) {
        this.restClient = restClient;
        this.cacheTtlNanos = cacheTtl.toNanos();
    }

    @Override
    public SiteSummary getSite(Integer siteId) {
        CachedSite cached = cache.get(siteId);
        if (cached != null && System.nanoTime() < cached.expiresAt()) {
            return cached.site();
        }
        SiteSummary site = fetch(siteId);
        cache.put(siteId, new CachedSite(site, System.nanoTime() + cacheTtlNanos));
        return site;
    }

    private SiteSummary fetch(Integer siteId) {
        SiteEnvelope body;
        try {
            body = restClient.get().uri(SITE_PATH, siteId).retrieve().body(SiteEnvelope.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("SITE_NOT_FOUND", "Site not found with id: " + siteId);
        } catch (RestClientException e) {
            log.warn("Geolocation service failed for site {}: {}", siteId, e.getMessage());
            throw unavailable();
        }
        if (body == null || body.data() == null) {
            log.warn("Geolocation service returned no data for site {}", siteId);
            throw unavailable();
        }
        SiteData data = body.data();
        return new SiteSummary(data.siteId(), data.name(), data.capacity());
    }

    private static ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("GEOLOCATION_UNAVAILABLE",
                "The geolocation service is not available, try again later");
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    /** Envoltura {@code ApiResponse} de la respuesta; solo interesa {@code data}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiteEnvelope(SiteData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiteData(Integer siteId, String name, Integer capacity) {
    }

    private record CachedSite(SiteSummary site, long expiresAt) {
    }
}
