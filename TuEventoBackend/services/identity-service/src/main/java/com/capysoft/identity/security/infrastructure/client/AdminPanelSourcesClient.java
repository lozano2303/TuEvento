package com.capysoft.identity.security.infrastructure.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.identity.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.identity.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.identity.shared.infrastructure.client.CommerceInternalApiConfig;
import com.capysoft.identity.shared.infrastructure.client.MonolithInternalApiConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Catalog and Commerce figures for the admin panel, through their internal APIs: Catalog's
 * ({@code /internal/admin-panel/**}) and commerce-service's ({@code /internal/admin/**}). If one does
 * not answer, 503: the panel shows the error instead of zeroed figures that would look real.
 */
@Slf4j
@Component
public class AdminPanelSourcesClient implements AdminPanelSourcesPort {

    private final RestClient catalog;
    private final RestClient commerce;

    public AdminPanelSourcesClient(@Qualifier(MonolithInternalApiConfig.BEAN) RestClient catalog,
                                   @Qualifier(CommerceInternalApiConfig.BEAN) RestClient commerce) {
        this.catalog = catalog;
        this.commerce = commerce;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data) {
    }

    @Override
    public CatalogStats catalogStats() {
        return get(catalog, "/internal/admin-panel/catalog-stats", new ParameterizedTypeReference<Envelope<CatalogStats>>() { });
    }

    @Override
    public long eventsPendingReview() {
        Long count = get(catalog, "/internal/admin-panel/events-pending-review", new ParameterizedTypeReference<Envelope<Long>>() { });
        return count == null ? 0 : count;
    }

    @Override
    public List<RecentEvent> recentlyUpdatedEvents(int limit) {
        return get(catalog, "/internal/admin-panel/recent-events?limit=" + limit,
                new ParameterizedTypeReference<Envelope<List<RecentEvent>>>() { });
    }

    @Override
    public CommerceStats commerceStats() {
        return get(commerce, "/internal/admin/stats", new ParameterizedTypeReference<Envelope<CommerceStats>>() { });
    }

    @Override
    public List<RecentPayment> recentPayments(int limit) {
        return get(commerce, "/internal/admin/recent-payments?limit=" + limit,
                new ParameterizedTypeReference<Envelope<List<RecentPayment>>>() { });
    }

    private static <T> T get(RestClient restClient, String path, ParameterizedTypeReference<Envelope<T>> type) {
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
