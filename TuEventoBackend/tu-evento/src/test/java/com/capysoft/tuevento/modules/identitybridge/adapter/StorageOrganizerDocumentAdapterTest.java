package com.capysoft.tuevento.modules.identitybridge.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.capysoft.tuevento.modules.identitybridge.port.OrganizerDocumentPort.DocumentUpload;
import com.capysoft.tuevento.modules.identitybridge.port.OrganizerDocumentPort.DocumentUrl;
import com.capysoft.tuevento.shared.application.storage.FileStorage;
import com.capysoft.tuevento.shared.application.storage.FileStorage.FileUpload;
import com.capysoft.tuevento.shared.application.storage.FileStorage.StoredFileRef;

@DisplayName("StorageOrganizerDocumentAdapter (Identidad → Archivos)")
class StorageOrganizerDocumentAdapterTest {

    private final FileStorage fileStorage = mock(FileStorage.class);
    private final StorageOrganizerDocumentAdapter adapter = new StorageOrganizerDocumentAdapter(fileStorage);

    @Test
    @DisplayName("sube el documento con su categoría y dueño, y devuelve solo el storedFileId")
    void uploadsDocument() {
        when(fileStorage.upload(any())).thenReturn(new StoredFileRef(31, "https://cdn/doc.pdf"));
        byte[] content = {1, 2, 3};

        Integer id = adapter.upload(new DocumentUpload("ORGANIZER_DOCUMENT", "ORGANIZER_PETITION", 7,
                "cedula.pdf", "application/pdf", content));

        assertThat(id).isEqualTo(31);
        ArgumentCaptor<FileUpload> sent = ArgumentCaptor.forClass(FileUpload.class);
        verify(fileStorage).upload(sent.capture());
        assertThat(sent.getValue().categoryCode()).isEqualTo("ORGANIZER_DOCUMENT");
        assertThat(sent.getValue().ownerEntityType()).isEqualTo("ORGANIZER_PETITION");
        assertThat(sent.getValue().uploadedBy()).isEqualTo(7);
        assertThat(sent.getValue().content()).isSameAs(content);
    }

    @Test
    @DisplayName("pide la URL con el usuario y rol de quien la solicita")
    void generatesUrl() {
        when(fileStorage.publicUrl(31, 1, "ADMIN")).thenReturn(new StoredFileRef(31, "https://cdn/doc.pdf"));

        assertThat(adapter.publicUrl(31, 1, "ADMIN")).isEqualTo(new DocumentUrl(31, "https://cdn/doc.pdf"));
    }
}
