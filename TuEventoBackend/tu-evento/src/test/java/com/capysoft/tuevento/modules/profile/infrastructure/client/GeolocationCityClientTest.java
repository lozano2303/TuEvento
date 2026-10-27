package com.capysoft.tuevento.modules.profile.infrastructure.client;

import com.capysoft.tuevento.modules.profile.application.port.out.CitySummary;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas de {@link GeolocationCityClient}: la lectura de la ciudad y la versión tolerante
 * {@code findCity} que usan los perfiles para mostrarse aunque geolocalización falle.
 */
@DisplayName("GeolocationCityClient")
class GeolocationCityClientTest {

    private static final String CITY_URL = "http://geo/api/v1/geolocation/cities/3";
    private static final String CITY_JSON = """
            {"success":true,"data":{"cityId":3,"departmentId":1,"departmentName":"Antioquia",
             "name":"Medellín","code":"05001"}}
            """;

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://geo");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GeolocationCityClient client =
            new GeolocationCityClient(builder.build(), Duration.ofMinutes(1));

    @Test
    @DisplayName("devuelve id, nombre y departamento de la ciudad")
    void returnsCity() {
        server.expect(requestTo(CITY_URL)).andRespond(withSuccess(CITY_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getCity(3)).isEqualTo(new CitySummary(3, "Medellín", "Antioquia"));
    }

    @Test
    @DisplayName("un 404 es CITY_NOT_FOUND")
    void notFound() {
        server.expect(requestTo(CITY_URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getCity(3))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "CITY_NOT_FOUND");
    }

    @Test
    @DisplayName("un error del servicio es GEOLOCATION_UNAVAILABLE")
    void serverError() {
        server.expect(requestTo(CITY_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.getCity(3)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("findCity sin id no llama al servicio")
    void findCityWithoutId() {
        assertThat(client.findCity(null)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("findCity devuelve la ciudad cuando existe")
    void findCityFound() {
        server.expect(requestTo(CITY_URL)).andRespond(withSuccess(CITY_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.findCity(3)).map(CitySummary::name).contains("Medellín");
    }

    @Test
    @DisplayName("findCity devuelve vacío con la ciudad inexistente o el servicio caído")
    void findCityTolerant() {
        server.expect(requestTo(CITY_URL)).andRespond(withResourceNotFound());
        assertThat(client.findCity(3)).isEqualTo(Optional.empty());

        server.reset();
        server.expect(requestTo(CITY_URL)).andRespond(withServerError());
        assertThat(client.findCity(3)).isEmpty();
    }

    @Test
    @DisplayName("el constructor de Spring traduce un fallo de red a 503")
    void springConstructor() {
        GeolocationCityClient real = new GeolocationCityClient(
                RestClient.builder(), "http://localhost:1", Duration.ofMinutes(1));

        assertThatThrownBy(() -> real.getCity(3)).isInstanceOf(ServiceUnavailableException.class);
    }
}
