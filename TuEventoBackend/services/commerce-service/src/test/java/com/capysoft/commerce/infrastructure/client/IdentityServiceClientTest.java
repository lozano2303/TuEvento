package com.capysoft.commerce.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.commerce.shared.domain.exception.ServiceUnavailableException;

@DisplayName("IdentityServiceClient")
class IdentityServiceClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://identity");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final IdentityServiceClient client = new IdentityServiceClient(builder.build());

    private void respond(String url, String json) {
        server.expect(requestTo("http://identity" + url)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("pide los resúmenes sin repetir ids ni nulos")
    void summaries() {
        respond("/internal/users/summary?ids=7,8", """
                {"data":[{"userId":7,"alias":"ana","email":"ana@example.com","fullName":"Ana","preferredLanguageId":1,"extra":true}]}
                """);

        assertThat(client.getUserSummaries(Arrays.asList(7, 7, null, 8))).containsOnlyKeys(7)
                .extractingByKey(7).satisfies(user -> assertThat(user.email()).isEqualTo("ana@example.com"));
    }

    @Test
    @DisplayName("cuenta y pagina los usuarios del crédito masivo")
    void users() {
        respond("/internal/users/count", "{\"data\":12}");
        respond("/internal/users/ids?afterUserId=3&limit=2", "{\"data\":[4,5]}");

        assertThat(client.countUsers()).isEqualTo(12);
        assertThat(client.findUserIdsAfter(3, 2)).isEqualTo(List.of(4, 5));
    }

    @Test
    @DisplayName("si Identidad falla o no devuelve datos responde 503")
    void unavailable() {
        server.expect(requestTo("http://identity/internal/users/count")).andRespond(withServerError());
        assertThatThrownBy(client::countUsers).isInstanceOf(ServiceUnavailableException.class);

        server.reset();
        respond("/internal/users/count", "{}");
        assertThatThrownBy(client::countUsers).isInstanceOf(ServiceUnavailableException.class);
    }
}
