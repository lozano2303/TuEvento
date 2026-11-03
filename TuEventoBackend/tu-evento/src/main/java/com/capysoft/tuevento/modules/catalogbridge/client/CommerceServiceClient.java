package com.capysoft.tuevento.modules.catalogbridge.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Client of commerce-service's internal API ({@code /internal/**}, with the service-to-service key):
 * the only place where Catalog asks Commerce for data. When Commerce does not answer it throws
 * {@link ServiceUnavailableException}, never an empty answer that would look real.
 */
@Slf4j
@Component
public class CommerceServiceClient {

    static final int MAX_IDS_PER_REQUEST = 500;
    static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    @Autowired
    public CommerceServiceClient(RestClient.Builder builder,
                                 @Value("${app.commerce.base-url:http://localhost:8084}") String baseUrl,
                                 @Value("${app.internal.api-key:}") String apiKey) {
        this(builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(timeouts()).build());
    }

    CommerceServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data) {
    }

    /** The subset of {@code seatIds} whose order still has its payment in progress. */
    public Set<Integer> seatsWithPendingPayment(Collection<Integer> seatIds) {
        Set<Integer> found = new HashSet<>();
        for (List<Integer> chunk : chunks(seatIds)) {
            found.addAll(post("/internal/seats/pending-payment", chunk,
                    new ParameterizedTypeReference<Envelope<Set<Integer>>>() { }));
        }
        return found;
    }

    /** Tickets sold (paid or used) per event; events Commerce does not report map to 0. */
    public Map<Long, Long> ticketsSold(Collection<Long> eventIds) {
        Map<Long, Long> sold = new HashMap<>();
        for (List<Long> chunk : chunks(eventIds)) {
            sold.putAll(post("/internal/events/tickets-sold", chunk,
                    new ParameterizedTypeReference<Envelope<Map<Long, Long>>>() { }));
        }
        return sold;
    }

    private <T> T post(String path, Object body, ParameterizedTypeReference<Envelope<T>> type) {
        try {
            Envelope<T> response = restClient.post().uri(path).body(body).retrieve().body(type);
            if (response == null || response.data() == null) {
                throw new ServiceUnavailableException("COMMERCE_UNAVAILABLE", "Commerce returned no data for " + path);
            }
            return response.data();
        } catch (RestClientException e) {
            log.warn("Commerce service unavailable ({}): {}", path, e.getMessage());
            throw new ServiceUnavailableException("COMMERCE_UNAVAILABLE",
                    "The commerce service is not available, try again later");
        }
    }

    private static <T> List<List<T>> chunks(Collection<T> ids) {
        List<T> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        List<List<T>> chunks = new ArrayList<>();
        for (int from = 0; from < distinct.size(); from += MAX_IDS_PER_REQUEST) {
            chunks.add(distinct.subList(from, Math.min(from + MAX_IDS_PER_REQUEST, distinct.size())));
        }
        return chunks;
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
