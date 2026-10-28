package com.capysoft.identity.security.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort.DocumentUpload;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort.DocumentUrl;
import com.capysoft.identity.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("MonolithOrganizerDocumentClient (Identidad → Archivos)")
class MonolithOrganizerDocumentClientTest {

    private static final DocumentUpload UPLOAD = new DocumentUpload("ORGANIZER_DOCUMENT", "ORGANIZER_PETITION", 7,
            "cedula.pdf", "application/pdf", new byte[] {1, 2, 3});

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://monolith");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final MonolithOrganizerDocumentClient client =
            new MonolithOrganizerDocumentClient(builder.build(), new ObjectMapper());

    @Test
    @DisplayName("sube el documento como multipart y devuelve el storedFileId")
    void uploads() {
        server.expect(requestTo("http://monolith/internal/organizer-documents")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"data\":{\"storedFileId\":31}}", MediaType.APPLICATION_JSON));

        assertThat(client.upload(UPLOAD)).isEqualTo(31);
        server.verify();
    }

    @Test
    @DisplayName("un rechazo de Archivos llega como error de negocio con su código; un fallo, como 503")
    void errors() {
        server.expect(requestTo("http://monolith/internal/organizer-documents"))
                .andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"code\":\"INVALID_FILE_TYPE\",\"message\":\"File type not allowed\"}"));
        server.expect(requestTo("http://monolith/internal/organizer-documents")).andRespond(withServerError());

        assertThatThrownBy(() -> client.upload(UPLOAD)).isInstanceOf(BusinessException.class)
                .hasMessage("File type not allowed")
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo("INVALID_FILE_TYPE"));
        assertThatThrownBy(() -> client.upload(UPLOAD)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    @DisplayName("pide la URL con el usuario y el rol de quien la solicita")
    void publicUrl() {
        server.expect(requestTo("http://monolith/internal/organizer-documents/31/url?requesterUserId=1&requesterRole=ADMIN"))
                .andRespond(withSuccess("{\"data\":{\"storedFileId\":31,\"publicUrl\":\"https://cdn/doc.pdf\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.publicUrl(31, 1, "ADMIN")).isEqualTo(new DocumentUrl(31, "https://cdn/doc.pdf"));
    }
}
