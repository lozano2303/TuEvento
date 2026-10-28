package com.capysoft.identity.profile.infrastructure.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.capysoft.identity.profile.application.port.out.CityLookupPort;
import com.capysoft.identity.profile.application.port.out.CitySummary;
import com.capysoft.identity.shared.infrastructure.client.RemoteResource;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lee las ciudades por la API pública de geolocalización ({@code GET /api/v1/geolocation/cities/{id}})
 * de geolocation-service. Misma URL y TTL que las sedes de eventos:
 * {@code GEOLOCATION_SERVICE_URL} o, sin ella, {@code http://localhost:8087}.
 */
@Component
public class GeolocationCityClient implements CityLookupPort {

    private static final RemoteResource.Errors ERRORS = new RemoteResource.Errors(
            "CITY_NOT_FOUND", "City", "GEOLOCATION_UNAVAILABLE", "geolocation");

    private final RemoteResource<CityData> cities;

    @Autowired
    public GeolocationCityClient(RestClient.Builder builder,
                                 @Value("${app.geolocation.base-url:${GEOLOCATION_SERVICE_URL:http://localhost:8087}}")
                                 String baseUrl,
                                 @Value("${app.geolocation.cache-ttl:PT1M}") Duration cacheTtl) {
        this(RemoteResource.restClient(builder, baseUrl), cacheTtl);
    }

    GeolocationCityClient(RestClient restClient, Duration cacheTtl) {
        this.cities = new RemoteResource<>(restClient, "/api/v1/geolocation/cities/{cityId}",
                new ParameterizedTypeReference<>() { }, ERRORS, cacheTtl);
    }

    @Override
    public CitySummary getCity(Integer cityId) {
        CityData data = cities.get(cityId);
        return new CitySummary(data.cityId(), data.name(), data.departmentName());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CityData(Integer cityId, String name, String departmentName) {
    }
}
