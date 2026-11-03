package com.capysoft.tuevento.modules.catalogbridge.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;

@DisplayName("CommerceServiceClient (catalog → commerce-service)")
class CommerceServiceClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://commerce")
            .defaultHeader("X-Internal-Api-Key", "secret");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CommerceServiceClient client = new CommerceServiceClient(builder.build());

    @Test
    @DisplayName("seats with a pending payment, with the service-to-service key")
    void seatsWithPendingPayment() {
        server.expect(requestTo("http://commerce/internal/seats/pending-payment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Api-Key", "secret"))
                .andExpect(content().json("[1,2,3]"))
                .andRespond(withSuccess("{\"data\":[2]}", MediaType.APPLICATION_JSON));

        assertThat(client.seatsWithPendingPayment(List.of(1, 2, 3, 2))).containsExactly(2);
        server.verify();
    }

    @Test
    @DisplayName("tickets sold per event, in chunks of 500 ids")
    void ticketsSoldInChunks() {
        List<Long> ids = LongStream.rangeClosed(1, 501).boxed().toList();
        server.expect(once(), requestTo("http://commerce/internal/events/tickets-sold"))
                .andRespond(withSuccess("{\"data\":{\"1\":4,\"2\":0}}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://commerce/internal/events/tickets-sold"))
                .andExpect(content().json("[501]"))
                .andRespond(withSuccess("{\"data\":{\"501\":7}}", MediaType.APPLICATION_JSON));

        assertThat(client.ticketsSold(ids)).containsExactlyInAnyOrderEntriesOf(Map.of(1L, 4L, 2L, 0L, 501L, 7L));
        server.verify();
    }

    @Test
    @DisplayName("an error from Commerce is a 503, never an empty answer")
    void unavailable() {
        server.expect(requestTo("http://commerce/internal/seats/pending-payment")).andRespond(withServerError());

        assertThatThrownBy(() -> client.seatsWithPendingPayment(List.of(1)))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("nothing to ask: no request at all")
    void emptyInput() {
        assertThat(client.seatsWithPendingPayment(List.of())).isEmpty();
        assertThat(client.ticketsSold(List.of())).isEmpty();
        server.verify();
    }
}
