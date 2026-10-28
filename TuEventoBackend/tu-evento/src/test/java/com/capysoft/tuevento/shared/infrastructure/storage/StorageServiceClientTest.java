package com.capysoft.tuevento.shared.infrastructure.storage;

import com.capysoft.tuevento.shared.application.storage.FileStorage.FileUpload;
import com.capysoft.tuevento.shared.application.storage.FileStorage.StoredFileRef;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.ImagePolicyViolationException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("StorageServiceClient (API interna de storage-service)")
class StorageServiceClientTest {

    private static final String FILE_JSON = "{\"data\":{\"storedFileId\":5,\"publicUrl\":\"https://cdn/a.jpg\"}}";

    private MockRestServiceServer server;
    private StorageServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://storage")
                .defaultHeader(StorageServiceClient.API_KEY_HEADER, "key");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new StorageServiceClient(builder.build(), new ObjectMapper());
    }

    private static FileUpload upload() {
        return new FileUpload("EVENT_MEDIA", 42, "cover.jpg", "image/jpeg", new byte[]{1}, "EVENT", 9);
    }

    private void respondError(HttpStatus status, String code) {
        server.expect(requestTo("http://storage/internal/files"))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"code\":\"" + code + "\",\"message\":\"from storage\"}"));
    }

    @Test
    @DisplayName("sube en multipart con la clave entre servicios, la categoría, el autor y el dueño")
    void uploads() {
        server.expect(requestTo("http://storage/internal/files")).andExpect(method(HttpMethod.POST))
                .andExpect(header(StorageServiceClient.API_KEY_HEADER, "key"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("name=\"file\"; filename=\"cover.jpg\""),
                        org.hamcrest.Matchers.containsString("name=\"categoryCode\""),
                        org.hamcrest.Matchers.containsString("EVENT_MEDIA"),
                        org.hamcrest.Matchers.containsString("name=\"ownerEntityType\""),
                        org.hamcrest.Matchers.containsString("name=\"ownerEntityId\""))))
                .andRespond(withSuccess(FILE_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.upload(upload())).isEqualTo(new StoredFileRef(5, "https://cdn/a.jpg"));
        server.verify();
    }

    @Test
    @DisplayName("sin dueño no manda los campos del dueño")
    void uploadsWithoutOwner() {
        server.expect(requestTo("http://storage/internal/files"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ownerEntity"))))
                .andRespond(withSuccess(FILE_JSON, MediaType.APPLICATION_JSON));

        client.upload(new FileUpload("ORGANIZER_DOCUMENT", 7, "doc.pdf", "application/pdf", new byte[]{1}, null, null));
        server.verify();
    }

    @Test
    @DisplayName("pide la URL en nombre del usuario y lee archivos y dueños")
    void reads() {
        server.expect(requestTo("http://storage/internal/files/31/url?requesterUserId=1&requesterRole=ADMIN"))
                .andRespond(withSuccess("{\"data\":{\"storedFileId\":31,\"publicUrl\":\"https://cdn/doc.pdf\"}}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://storage/internal/files/5"))
                .andRespond(withSuccess(FILE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://storage/internal/files/owner/DEFAULT_AVATAR/0"))
                .andRespond(withSuccess("{\"data\":[{\"storedFileId\":1},{\"storedFileId\":2}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.publicUrl(31, 1, "ADMIN")).isEqualTo(new StoredFileRef(31, "https://cdn/doc.pdf"));
        assertThat(client.findPublicUrl(5)).contains("https://cdn/a.jpg");
        assertThat(client.findIdsByOwner("DEFAULT_AVATAR", 0)).containsExactly(1, 2);
    }

    @Test
    @DisplayName("un archivo inexistente es un Optional vacío al buscar su URL")
    void missingFile() {
        server.expect(requestTo("http://storage/internal/files/8"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"FILE_NOT_FOUND\",\"message\":\"nope\"}"));

        assertThat(client.findPublicUrl(8)).isEmpty();
    }

    @Test
    @DisplayName("los errores del servicio llegan al usuario con su código")
    void translatesClientErrors() {
        respondError(HttpStatus.NOT_FOUND, "FILE_CATEGORY_NOT_FOUND");
        assertThatThrownBy(() -> client.upload(upload())).isInstanceOf(NotFoundException.class)
                .hasMessage("from storage");
        server.reset();

        respondError(HttpStatus.FORBIDDEN, "STORAGE_ACCESS_DENIED");
        assertThatThrownBy(() -> client.upload(upload())).isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("STORAGE_ACCESS_DENIED");
        server.reset();

        respondError(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGE_POLICY_VIOLATION");
        assertThatThrownBy(() -> client.upload(upload())).isInstanceOf(ImagePolicyViolationException.class);
        server.reset();

        respondError(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE");
        assertThatThrownBy(() -> client.upload(upload())).isExactlyInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    @DisplayName("un 4xx sin cuerpo legible queda como STORAGE_ERROR")
    void unreadableErrorBody() {
        server.expect(requestTo("http://storage/internal/files"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("not json"));

        assertThatThrownBy(() -> client.upload(upload())).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("STORAGE_ERROR");
    }

    @Test
    @DisplayName("una clave rechazada, un 5xx, un fallo de red o una respuesta vacía son STORAGE_UNAVAILABLE")
    void unavailable() {
        respondError(HttpStatus.UNAUTHORIZED, "INTERNAL_AUTH_REQUIRED");
        assertUnavailable();
        server.reset();

        server.expect(requestTo("http://storage/internal/files")).andRespond(withServerError());
        assertUnavailable();
        server.reset();

        server.expect(requestTo("http://storage/internal/files")).andRespond(withException(new IOException("refused")));
        assertUnavailable();
        server.reset();

        server.expect(requestTo("http://storage/internal/files"))
                .andRespond(withSuccess("{\"data\":null}", MediaType.APPLICATION_JSON));
        assertUnavailable();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> client.upload(upload())).isInstanceOf(ServiceUnavailableException.class)
                .extracting("code").isEqualTo("STORAGE_UNAVAILABLE");
    }
}
