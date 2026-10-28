package com.capysoft.tuevento.modules.identitybridge.client;

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
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

@DisplayName("IdentityServiceClient (monolito → identity-service)")
class IdentityServiceClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://identity")
            .defaultHeader("X-Internal-Api-Key", "secret");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final IdentityServiceClient client = new IdentityServiceClient(builder.build());

    @Test
    @DisplayName("resúmenes de usuarios con la clave entre servicios, en bloques de 500 IDs")
    void summariesInChunks() {
        List<Integer> ids = IntStream.rangeClosed(1, 501).boxed().toList();
        server.expect(once(), request -> assertThat(request.getURI().toString())
                        .startsWith("http://identity/internal/users/summary?ids=1,2,").endsWith(",500"))
                .andExpect(header("X-Internal-Api-Key", "secret"))
                .andRespond(withSuccess("{\"data\":[{\"userId\":7,\"alias\":\"ana\",\"fullName\":\"Ana\"}]}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://identity/internal/users/summary?ids=501"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.getUserSummaries(ids)).containsOnlyKeys(7);
        server.verify();
    }

    @Test
    @DisplayName("conteo, IDs por cursor, solicitudes pendientes y estado de una traducción")
    void otherReads() {
        server.expect(requestTo("http://identity/internal/users/count"))
                .andRespond(withSuccess("{\"data\":12}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/users/ids?afterUserId=5&limit=2"))
                .andRespond(withSuccess("{\"data\":[6,7]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/admin/organizer-requests/pending-count"))
                .andRespond(withSuccess("{\"data\":3}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/profiles/30/translations/en/status"))
                .andRespond(withSuccess("{\"data\":{\"status\":\"PUBLISHED\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/profiles/default-avatar?storedFileId=1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"data\":4}", MediaType.APPLICATION_JSON));

        assertThat(client.countUsers()).isEqualTo(12);
        assertThat(client.findUserIdsAfter(5, 2)).containsExactly(6, 7);
        assertThat(client.pendingOrganizerRequests()).isEqualTo(3);
        assertThat(client.profileTranslationStatus(30L, "en")).contains(TranslationStatus.PUBLISHED);
        assertThat(client.assignDefaultAvatar(1)).isEqualTo(4);
        server.verify();
    }

    @Test
    @DisplayName("identity-service caído → 503 (no un dato vacío)")
    void unavailable() {
        server.expect(requestTo("http://identity/internal/users/count")).andRespond(withServerError());

        assertThatThrownBy(client::countUsers).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("reads the profile texts to translate and saves the translation back")
    void profileTranslations() {
        server.expect(requestTo("http://identity/internal/profiles/30/translatable-texts"))
                .andRespond(withSuccess("{\"data\":{\"biography\":\"Hola\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/profiles/30/translations/en"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"texts\":{\"biography\":\"Hello\"},\"source\":\"MACHINE\",\"status\":\"PUBLISHED\"}"))
                .andRespond(withSuccess());

        assertThat(client.profileTranslatableTexts(30L)).containsEntry("biography", "Hola");
        client.saveProfileTranslation(30L, "en", Map.of("biography", "Hello"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        server.verify();
    }

    @Test
    @DisplayName("an empty answer is not taken as data: no status, zero avatars, and 503 for reads")
    void emptyAnswers() {
        server.expect(requestTo("http://identity/internal/profiles/30/translations/en/status"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/profiles/default-avatar?storedFileId=1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://identity/internal/users/count"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.profileTranslationStatus(30L, "en")).isEmpty();
        assertThat(client.assignDefaultAvatar(1)).isZero();
        assertThatThrownBy(client::countUsers).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("identity-service down on writes and status reads surfaces as 503 too")
    void writesUnavailable() {
        server.expect(requestTo("http://identity/internal/profiles/30/translations/en")).andRespond(withServerError());
        server.expect(requestTo("http://identity/internal/profiles/30/translations/en/status")).andRespond(withServerError());
        server.expect(requestTo("http://identity/internal/profiles/default-avatar?storedFileId=1")).andRespond(withServerError());

        assertThatThrownBy(() -> client.saveProfileTranslation(30L, "en", Map.of(), TranslationSource.MACHINE,
                TranslationStatus.PUBLISHED)).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> client.profileTranslationStatus(30L, "en")).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> client.assignDefaultAvatar(1)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("the Spring constructor uses short timeouts: an unreachable identity-service fails fast with 503")
    void springConstructorFailsFast() {
        IdentityServiceClient unreachable = new IdentityServiceClient(RestClient.builder(), "http://127.0.0.1:1", "key");

        assertThatThrownBy(unreachable::countUsers).isInstanceOf(ServiceUnavailableException.class);
    }
}
