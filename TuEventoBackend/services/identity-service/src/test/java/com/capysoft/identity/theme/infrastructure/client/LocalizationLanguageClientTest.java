package com.capysoft.identity.theme.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("LocalizationLanguageClient (Identidad → Localización)")
class LocalizationLanguageClientTest {

    private static final String LANGUAGES = """
            {"success":true,"data":[{"languageId":1,"code":"es","name":"Español"},
                                    {"languageId":2,"code":"en","name":"English"}]}""";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://localization");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    @DisplayName("resuelve el id por código, sin distinguir mayúsculas, con una sola llamada mientras dura el TTL")
    void resolvesAndCaches() {
        server.expect(once(), requestTo("http://localization/api/v1/languages"))
                .andRespond(withSuccess(LANGUAGES, MediaType.APPLICATION_JSON));
        LocalizationLanguageClient client = new LocalizationLanguageClient(builder.build(), Duration.ofMinutes(10));

        assertThat(client.findIdByCode("EN")).contains(2);
        assertThat(client.findIdByCode("es")).contains(1);
        assertThat(client.findIdByCode("xx")).isEmpty();
        assertThat(client.findIdByCode(null)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("Localización caída → sin idioma (los temas salen sin traducir) y sin guardar el error")
    void unavailableReturnsEmpty() {
        server.expect(once(), requestTo("http://localization/api/v1/languages")).andRespond(withServerError());
        server.expect(once(), requestTo("http://localization/api/v1/languages"))
                .andRespond(withSuccess(LANGUAGES, MediaType.APPLICATION_JSON));
        LocalizationLanguageClient client = new LocalizationLanguageClient(builder.build(), Duration.ofMinutes(10));

        assertThat(client.findIdByCode("en")).isEmpty();
        assertThat(client.findIdByCode("en")).contains(2);
        server.verify();
    }
}
