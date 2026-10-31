package com.capysoft.commerce.infrastructure.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.commerce.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * API interna de Catálogo (sección 4.1 del plan). {@code CATALOG_SERVICE_URL} apunta al monolito
 * mientras Catálogo siga allí, y a catalog-service cuando exista: el contrato es el mismo.
 */
@Slf4j
@Component
public class CatalogServiceClient {

    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    @Autowired
    public CatalogServiceClient(RestClient.Builder builder,
                                @Value("${app.catalog.base-url:http://localhost:8080}") String baseUrl,
                                @Value("${app.internal.api-key:}") String apiKey) {
        this(builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(timeouts()).build());
    }

    CatalogServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    record HoldRequest(Long orderId, Long userId, Long eventId, List<Integer> seatIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeldSeat(Integer seatId, Integer eventSectionId, BigDecimal price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeldSeats(String currency, List<HeldSeat> seats) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventSeatSummary(Long eventId, String eventName, BigDecimal potentialRevenue,
                                   Long courtesySeats, Long totalSeats) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data, String code, String message) {
    }

    /** @throws IllegalStateException con el motivo, si Catálogo rechaza las sillas (409) */
    public HeldSeats hold(Long orderId, Long userId, Long eventId, List<Integer> seatIds) {
        try {
            Envelope<HeldSeats> body = restClient.post().uri("/internal/seats/hold")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new HoldRequest(orderId, userId, eventId, seatIds))
                    .retrieve().body(new ParameterizedTypeReference<Envelope<HeldSeats>>() { });
            return dataOf(body, "hold seats");
        } catch (HttpClientErrorException.Conflict e) {
            throw new IllegalStateException(rejectionMessage(e));
        } catch (RestClientException e) {
            throw unavailable("hold seats", e);
        }
    }

    public List<EventSeatSummary> seatSummary(Long organizerId) {
        try {
            Envelope<List<EventSeatSummary>> body = restClient.get()
                    .uri("/internal/organizers/{id}/seat-summary", organizerId)
                    .retrieve().body(new ParameterizedTypeReference<Envelope<List<EventSeatSummary>>>() { });
            return dataOf(body, "seat summary");
        } catch (RestClientException e) {
            throw unavailable("seat summary", e);
        }
    }

    private static <T> T dataOf(Envelope<T> body, String what) {
        if (body == null || body.data() == null) {
            throw new ServiceUnavailableException("CATALOG_UNAVAILABLE", "Catalog returned no data for " + what);
        }
        return body.data();
    }

    private static String rejectionMessage(HttpClientErrorException e) {
        try {
            Envelope<?> body = e.getResponseBodyAs(Envelope.class);
            if (body != null && body.message() != null) {
                return body.message();
            }
        } catch (RuntimeException unreadable) {
            log.debug("Unreadable rejection from catalog: {}", unreadable.getMessage());
        }
        return "The seats could not be held";
    }

    private static ServiceUnavailableException unavailable(String what, Exception e) {
        log.warn("Catalog unavailable ({}): {}", what, e.getMessage());
        return new ServiceUnavailableException("CATALOG_UNAVAILABLE",
                "The catalog service is not available, try again later");
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
