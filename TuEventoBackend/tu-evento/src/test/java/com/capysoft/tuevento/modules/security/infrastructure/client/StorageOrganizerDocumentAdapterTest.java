package com.capysoft.tuevento.modules.security.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort.DocumentUpload;
import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort.DocumentUrl;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;

@DisplayName("StorageOrganizerDocumentAdapter (Identidad → Archivos)")
class StorageOrganizerDocumentAdapterTest {

    private final GeneratePublicUrlPort urls = mock(GeneratePublicUrlPort.class);
    private final UploadFilePort uploads = mock(UploadFilePort.class);
    private final StorageOrganizerDocumentAdapter adapter = new StorageOrganizerDocumentAdapter(urls, uploads);

    @Test
    @DisplayName("sube el documento con su categoría y dueño, y devuelve solo el storedFileId")
    void uploadsDocument() {
        when(uploads.upload(any())).thenReturn(UploadFileResponse.builder().storedFileId(31).build());
        byte[] content = {1, 2, 3};

        Integer id = adapter.upload(new DocumentUpload("ORGANIZER_DOCUMENT", "ORGANIZER_PETITION", 7,
                "cedula.pdf", "application/pdf", content));

        assertThat(id).isEqualTo(31);
        ArgumentCaptor<UploadFileRequest> sent = ArgumentCaptor.forClass(UploadFileRequest.class);
        verify(uploads).upload(sent.capture());
        assertThat(sent.getValue().getFileCategoryCode()).isEqualTo("ORGANIZER_DOCUMENT");
        assertThat(sent.getValue().getOwnerEntityType()).isEqualTo("ORGANIZER_PETITION");
        assertThat(sent.getValue().getUploadedBy()).isEqualTo(7);
        assertThat(sent.getValue().getContent()).isSameAs(content);
    }

    @Test
    @DisplayName("pide la URL con el usuario y rol de quien la solicita")
    void generatesUrl() {
        when(urls.generate(eq(31), any())).thenReturn(
                PublicUrlResponse.builder().storedFileId(31).publicUrl("https://cdn/doc.pdf").build());

        assertThat(adapter.publicUrl(31, 1, "ADMIN")).isEqualTo(new DocumentUrl(31, "https://cdn/doc.pdf"));
    }
}
