package com.capysoft.identity.security.infrastructure.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.identity.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.identity.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.identity.shared.infrastructure.client.MonolithInternalApiConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Datos de Catálogo y Comercio para el panel de admin, por la API interna del monolito
 * ({@code /internal/admin-panel/**}). Si no responde, 503: el panel muestra el error en vez de
 * estadísticas en cero que parecerían reales.
 */
@Slf4j
@Component
public class MonolithAdminPanelClient implements AdminPanelSourcesPort {

    private final RestClient restClient;

    public MonolithAdminPanelClient(@Qualifier(MonolithInternalApiConfig.BEAN) RestClient restClient) {
        this.restClient = restClient;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data) {
    }

    @Override
    public CatalogStats catalogStats() {
        return get("/internal/admin-panel/catalog-stats", new ParameterizedTypeReference<Envelope<CatalogStats>>() { });
    }

    @Override
    public long eventsPendingReview() {
        Long count = get("/internal/admin-panel/events-pending-review", new ParameterizedTypeReference<Envelope<Long>>() { });
        return count == null ? 0 : count;
    }

    @Override
    public List<RecentEvent> recentlyUpdatedEvents(int limit) {
        return get("/internal/admin-panel/recent-events?limit=" + limit,
                new ParameterizedTypeReference<Envelope<List<RecentEvent>>>() { });
    }

    @Override
    public CommerceStats commerceStats() {
        return get("/internal/admin-panel/commerce-stats", new ParameterizedTypeReference<Envelope<CommerceStats>>() { });
    }

    @Override
    public List<RecentPayment> recentPayments(int limit) {
        return get("/internal/admin-panel/recent-payments?limit=" + limit,
                new ParameterizedTypeReference<Envelope<List<RecentPayment>>>() { });
    }

    private <T> T get(String path, ParameterizedTypeReference<Envelope<T>> type) {
        try {
            Envelope<T> body = restClient.get().uri(path).retrieve().body(type);
            if (body == null || body.data() == null) {
                throw unavailable(path, "empty response");
            }
            return body.data();
        } catch (RestClientException e) {
            throw unavailable(path, e.getMessage());
        }
    }

    private static ServiceUnavailableException unavailable(String path, String reason) {
        log.warn("Admin panel source {} unavailable: {}", path, reason);
        return new ServiceUnavailableException("ADMIN_PANEL_UNAVAILABLE",
                "Admin panel data is not available right now, try again later");
    }
}
