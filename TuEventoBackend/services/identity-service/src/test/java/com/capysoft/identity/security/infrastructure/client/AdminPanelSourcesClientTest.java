package com.capysoft.identity.security.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.identity.shared.domain.exception.ServiceUnavailableException;

@DisplayName("AdminPanelSourcesClient (admin panel → Catalog and Commerce)")
class AdminPanelSourcesClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://monolith")
            .defaultHeader("X-Internal-Api-Key", "secret");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestClient.Builder commerceBuilder = RestClient.builder().baseUrl("http://commerce")
            .defaultHeader("X-Internal-Api-Key", "secret");
    private final MockRestServiceServer commerceServer = MockRestServiceServer.bindTo(commerceBuilder).build();
    private final AdminPanelSourcesClient client = new AdminPanelSourcesClient(builder.build(), commerceBuilder.build());

    @Test
    @DisplayName("lee estadísticas, pendientes y actividad con la clave entre servicios")
    void readsSources() {
        server.expect(requestTo("http://monolith/internal/admin-panel/catalog-stats"))
                .andExpect(header("X-Internal-Api-Key", "secret"))
                .andRespond(withSuccess("{\"data\":{\"total\":9,\"published\":4,\"pendingReview\":2,\"rejected\":1,"
                        + "\"cancelled\":1,\"completed\":0,\"draft\":1}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://monolith/internal/admin-panel/events-pending-review"))
                .andRespond(withSuccess("{\"data\":2}", MediaType.APPLICATION_JSON));
        commerceServer.expect(requestTo("http://commerce/internal/admin/stats"))
                .andExpect(header("X-Internal-Api-Key", "secret"))
                .andRespond(withSuccess("{\"data\":{\"totalTickets\":5,\"totalRevenue\":120.50,\"approvedPayments\":3,"
                        + "\"pendingRefunds\":1,\"totalRefundedAmount\":10,\"processedRefunds\":1}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://monolith/internal/admin-panel/recent-events?limit=5"))
                .andRespond(withSuccess("{\"data\":[{\"eventName\":\"Concierto\",\"status\":\"PUBLISHED\","
                        + "\"organizerUserId\":4,\"occurredAt\":\"2026-10-27T10:00:00\"}]}", MediaType.APPLICATION_JSON));

        assertThat(client.catalogStats().published()).isEqualTo(4);
        assertThat(client.eventsPendingReview()).isEqualTo(2);
        assertThat(client.commerceStats().totalRevenue()).isEqualByComparingTo(new BigDecimal("120.50"));
        assertThat(client.recentlyUpdatedEvents(5)).singleElement()
                .satisfies(e -> assertThat(e.eventName()).isEqualTo("Concierto"));
        server.verify();
        commerceServer.verify();
    }

    @Test
    @DisplayName("monolito caído → 503 (no estadísticas en cero que parecerían reales)")
    void unavailable() {
        server.expect(requestTo("http://monolith/internal/admin-panel/catalog-stats")).andRespond(withServerError());

        assertThatThrownBy(client::catalogStats).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("recent payments come from commerce-service; if it is down, 503")
    void recentPaymentsFromCommerce() {
        commerceServer.expect(requestTo("http://commerce/internal/admin/recent-payments?limit=3"))
                .andRespond(withSuccess("{\"data\":[{\"approved\":true,\"amount\":10,\"currency\":\"COP\","
                        + "\"orderId\":5,\"processedAt\":\"2026-10-27T10:00:00\"}]}", MediaType.APPLICATION_JSON));
        commerceServer.expect(requestTo("http://commerce/internal/admin/stats")).andRespond(withServerError());

        assertThat(client.recentPayments(3)).singleElement().satisfies(p -> assertThat(p.orderId()).isEqualTo(5L));
        assertThatThrownBy(client::commerceStats).isInstanceOf(ServiceUnavailableException.class);
        commerceServer.verify();
    }
}
