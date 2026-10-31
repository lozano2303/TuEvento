package com.capysoft.commerce.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.commerce.shared.domain.exception.ServiceUnavailableException;

@DisplayName("CatalogServiceClient")
class CatalogServiceClientTest {

    private static final String HOLD = "http://catalog/internal/seats/hold";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CatalogServiceClient client = new CatalogServiceClient(builder.build());

    private CatalogServiceClient.HeldSeats hold() {
        return client.hold(9L, 5L, 3L, List.of(1));
    }

    @Test
    @DisplayName("hold envía el contrato y devuelve precio y moneda")
    void holds() {
        server.expect(requestTo(HOLD))
                .andExpect(content().json("{\"orderId\":9,\"userId\":5,\"eventId\":3,\"seatIds\":[1]}"))
                .andRespond(withSuccess("""
                        {"data":{"currency":"COP","seats":[{"seatId":1,"eventSectionId":2,"price":1000}]}}
                        """, MediaType.APPLICATION_JSON));

        var held = hold();

        assertThat(held.currency()).isEqualTo("COP");
        assertThat(held.seats()).singleElement().satisfies(seat -> assertThat(seat.price()).isEqualByComparingTo("1000"));
    }

    @Test
    @DisplayName("un 409 se traduce en el mismo error de negocio que antes, con su motivo")
    void rejected() {
        server.expect(requestTo(HOLD)).andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"SEAT_NOT_RESERVED\",\"message\":\"Seat 1 is not reserved by this user\"}"));

        assertThatThrownBy(this::hold).isInstanceOf(IllegalStateException.class)
                .hasMessage("Seat 1 is not reserved by this user");
    }

    @Test
    @DisplayName("un 409 sin cuerpo legible usa un motivo genérico")
    void rejectedUnreadable() {
        server.expect(requestTo(HOLD)).andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("nope"));

        assertThatThrownBy(this::hold).isInstanceOf(IllegalStateException.class).hasMessage("The seats could not be held");
    }

    @Test
    @DisplayName("si Catálogo falla o no devuelve datos responde 503")
    void unavailable() {
        server.expect(requestTo(HOLD)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(this::hold).isInstanceOf(ServiceUnavailableException.class);

        server.reset();
        server.expect(requestTo(HOLD)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(this::hold).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("seat-summary lee el resumen del organizador, y sin Catálogo responde 503")
    void seatSummary() {
        String url = "http://catalog/internal/organizers/7/seat-summary";
        server.expect(requestTo(url)).andRespond(withSuccess("""
                {"data":[{"eventId":3,"eventName":"Concierto","potentialRevenue":10,"courtesySeats":1,"totalSeats":2}]}
                """, MediaType.APPLICATION_JSON));
        assertThat(client.seatSummary(7L)).singleElement()
                .satisfies(row -> assertThat(row.eventName()).isEqualTo("Concierto"));

        server.reset();
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThatThrownBy(() -> client.seatSummary(7L)).isInstanceOf(ServiceUnavailableException.class);
    }
}
