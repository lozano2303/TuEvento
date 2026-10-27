package com.capysoft.tuevento.modules.event.infrastructure.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.modules.event.application.port.out.SiteSummary;
import com.capysoft.tuevento.shared.infrastructure.client.RemoteResource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lee las sedes por la API pública de geolocalización ({@code GET /api/v1/geolocation/sites/{id}})
 * en vez de importar el módulo: así el evento no depende del código de localización. La URL sale
 * de {@code GEOLOCATION_SERVICE_URL} (el microservicio); sin ella apunta al propio monolito.
 *
 * <p>Las sedes casi no cambian y los listados piden la misma sede por cada evento, así que se
 * guardan en memoria durante {@code app.geolocation.cache-ttl}.
 */
@Component
public class GeolocationSiteClient implements SiteLookupPort {

    private static final RemoteResource.Errors ERRORS = new RemoteResource.Errors(
            "SITE_NOT_FOUND", "Site", "GEOLOCATION_UNAVAILABLE", "geolocation");

    private final RemoteResource<SiteData> sites;

    @Autowired
    public GeolocationSiteClient(RestClient.Builder builder,
                                 @Value("${app.geolocation.base-url:${GEOLOCATION_SERVICE_URL:http://localhost:${server.port:8080}}}")
                                 String baseUrl,
                                 @Value("${app.geolocation.cache-ttl:PT1M}") Duration cacheTtl) {
        this(RemoteResource.restClient(builder, baseUrl), cacheTtl);
    }

    GeolocationSiteClient(RestClient restClient, Duration cacheTtl) {
        this.sites = new RemoteResource<>(restClient, "/api/v1/geolocation/sites/{siteId}",
                new ParameterizedTypeReference<>() { }, ERRORS, cacheTtl);
    }

    @Override
    public SiteSummary getSite(Integer siteId) {
        SiteData data = sites.get(siteId);
        return new SiteSummary(data.siteId(), data.name(), data.capacity());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiteData(Integer siteId, String name, Integer capacity) {
    }
}
