package com.capysoft.tuevento.modules.event.infrastructure.client;

import com.capysoft.tuevento.modules.event.application.port.out.SiteSummary;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas de {@link GeolocationSiteClient} contra un servidor HTTP simulado: lectura de la
 * envoltura {@code ApiResponse}, caché, sede inexistente y servicio caído.
 */
@DisplayName("GeolocationSiteClient")
class GeolocationSiteClientTest {

    private static final String SITE_URL = "http://geo/api/v1/geolocation/sites/7";
    private static final String SITE_JSON = """
            {"success":true,"message":"Site retrieved successfully","data":
              {"siteId":7,"cityId":1,"cityName":"Bogotá","name":"Movistar Arena",
               "address":"Calle 1","capacity":14000,"latitude":4.6,"longitude":-74.0}}
            """;

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://geo");
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private GeolocationSiteClient client(Duration ttl) {
        return new GeolocationSiteClient(builder.build(), ttl);
    }

    @Test
    @DisplayName("devuelve id, nombre y capacidad de la sede")
    void returnsSite() {
        server.expect(requestTo(SITE_URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SITE_JSON, MediaType.APPLICATION_JSON));

        SiteSummary site = client(Duration.ofMinutes(1)).getSite(7);

        assertThat(site).isEqualTo(new SiteSummary(7, "Movistar Arena", 14000));
        server.verify();
    }

    @Test
    @DisplayName("la segunda consulta dentro del TTL sale de la caché")
    void cachesWithinTtl() {
        server.expect(ExpectedCount.once(), requestTo(SITE_URL))
                .andRespond(withSuccess(SITE_JSON, MediaType.APPLICATION_JSON));
        GeolocationSiteClient client = client(Duration.ofMinutes(1));

        client.getSite(7);
        SiteSummary second = client.getSite(7);

        assertThat(second.name()).isEqualTo("Movistar Arena");
        server.verify();
    }

    @Test
    @DisplayName("con el TTL vencido vuelve a pedir la sede")
    void refetchesAfterTtl() {
        server.expect(ExpectedCount.twice(), requestTo(SITE_URL))
                .andRespond(withSuccess(SITE_JSON, MediaType.APPLICATION_JSON));
        GeolocationSiteClient client = client(Duration.ZERO);

        client.getSite(7);
        client.getSite(7);

        server.verify();
    }

    @Test
    @DisplayName("un 404 es SITE_NOT_FOUND")
    void notFound() {
        server.expect(requestTo(SITE_URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client(Duration.ofMinutes(1)).getSite(7))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "SITE_NOT_FOUND");
    }

    @Test
    @DisplayName("un error del servicio es GEOLOCATION_UNAVAILABLE, no sede inexistente")
    void serverError() {
        server.expect(requestTo(SITE_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client(Duration.ofMinutes(1)).getSite(7))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasFieldOrPropertyWithValue("code", "GEOLOCATION_UNAVAILABLE");
    }

    @Test
    @DisplayName("una respuesta sin data es GEOLOCATION_UNAVAILABLE y no se guarda en caché")
    void emptyData() {
        server.expect(ExpectedCount.twice(), requestTo(SITE_URL))
                .andRespond(withSuccess("{\"success\":true,\"data\":null}", MediaType.APPLICATION_JSON));
        GeolocationSiteClient client = client(Duration.ofMinutes(1));

        assertThatThrownBy(() -> client.getSite(7)).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> client.getSite(7)).isInstanceOf(ServiceUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("el constructor de Spring usa la URL base configurada")
    void springConstructorUsesBaseUrl() {
        RestClient.Builder springBuilder = RestClient.builder();
        GeolocationSiteClient client =
                new GeolocationSiteClient(springBuilder, "http://localhost:1", Duration.ofMinutes(1));
        // Nada escucha en el puerto 1: la conexión se rechaza al instante y el fallo de red
        // se traduce a 503 en vez de a sede inexistente.
        assertThatThrownBy(() -> client.getSite(7)).isInstanceOf(ServiceUnavailableException.class);
    }
}
