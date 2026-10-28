package com.capysoft.tuevento.modules.identitybridge.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.capysoft.tuevento.modules.identitybridge.port.OrganizerDocumentPort;
import com.capysoft.tuevento.modules.identitybridge.port.OrganizerDocumentPort.DocumentUpload;
import com.capysoft.tuevento.modules.identitybridge.port.OrganizerDocumentPort.DocumentUrl;

@DisplayName("InternalOrganizerDocumentController")
class InternalOrganizerDocumentControllerTest {

    private final OrganizerDocumentPort port = mock(OrganizerDocumentPort.class);
    private final InternalOrganizerDocumentController controller = new InternalOrganizerDocumentController(port);

    @Test
    @DisplayName("uploads the file through the storage port and returns its storedFileId")
    void uploads() throws IOException {
        DocumentUpload expected = new DocumentUpload("ORGANIZER_DOCUMENT", "organizer_petition", 7, "id.pdf",
                "application/pdf", new byte[] {1, 2, 3});
        when(port.upload(expected)).thenReturn(99);

        var response = controller.upload("ORGANIZER_DOCUMENT", "organizer_petition", 7,
                new MockMultipartFile("file", "id.pdf", "application/pdf", new byte[] {1, 2, 3}));

        assertThat(response.getBody().getData().storedFileId()).isEqualTo(99);
    }

    @Test
    @DisplayName("returns the document URL generated for the requester")
    void publicUrl() {
        DocumentUrl url = new DocumentUrl(99, "https://files.example.com/id.pdf");
        when(port.publicUrl(99, 1, "ADMIN")).thenReturn(url);

        assertThat(controller.publicUrl(99, 1, "ADMIN").getBody().getData()).isEqualTo(url);
        verify(port).publicUrl(99, 1, "ADMIN");
    }
}
