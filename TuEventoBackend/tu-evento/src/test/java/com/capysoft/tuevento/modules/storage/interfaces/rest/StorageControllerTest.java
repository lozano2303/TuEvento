package com.capysoft.tuevento.modules.storage.interfaces.rest;

import com.capysoft.tuevento.modules.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.port.in.CheckFileAccessPort;
import com.capysoft.tuevento.modules.storage.application.port.in.DeleteFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * El controlador consulta el guardia de acceso antes de exponer o borrar un archivo
 * y usa siempre el usuario autenticado, nunca un id enviado por el cliente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StorageController")
class StorageControllerTest {

    private static final Integer FILE_ID = 5;
    private static final SecurityUser USER = new SecurityUser(10, "user", "USER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");

    @Mock private UploadFilePort uploadFilePort;
    @Mock private DeleteFilePort deleteFilePort;
    @Mock private GetFilePort getFilePort;
    @Mock private GetFilesByOwnerPort getFilesByOwnerPort;
    @Mock private GeneratePublicUrlPort generatePublicUrlPort;
    @Mock private CheckFileAccessPort checkFileAccessPort;
    @InjectMocks private StorageController controller;

    @Test
    @DisplayName("upload → el archivo queda a nombre del usuario autenticado")
    void upload_usesAuthenticatedUser() throws Exception {
        controller.upload(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}),
                "ORGANIZER_DOCUMENT", null, null, USER);

        ArgumentCaptor<UploadFileRequest> request = ArgumentCaptor.forClass(UploadFileRequest.class);
        verify(uploadFilePort).upload(request.capture());
        assertThat(request.getValue().getUploadedBy()).isEqualTo(10);
        assertThat(request.getValue().getFileCategoryCode()).isEqualTo("ORGANIZER_DOCUMENT");
    }

    @Test
    @DisplayName("delete → primero el guardia, luego el borrado a nombre del usuario")
    void delete_checksAccessFirst() {
        controller.delete(FILE_ID, USER);

        InOrder order = inOrder(checkFileAccessPort, deleteFilePort);
        order.verify(checkFileAccessPort).assertCanDelete(FILE_ID, 10, false);
        ArgumentCaptor<DeleteFileRequest> request = ArgumentCaptor.forClass(DeleteFileRequest.class);
        order.verify(deleteFilePort).delete(request.capture());
        assertThat(request.getValue().getDeletedBy()).isEqualTo(10);
    }

    @Test
    @DisplayName("delete denegado → no se borra nada")
    void delete_denied_doesNotDelete() {
        doThrow(new AccessDeniedException("no")).when(checkFileAccessPort).assertCanDelete(FILE_ID, 10, false);

        assertThatThrownBy(() -> controller.delete(FILE_ID, USER)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(deleteFilePort);
    }

    @Test
    @DisplayName("info y URL firmada → pasan por el guardia de lectura (admin incluido)")
    void read_checksAccessFirst() {
        controller.getFile(FILE_ID, USER);
        controller.generateUrl(FILE_ID, ADMIN);

        InOrder order = inOrder(checkFileAccessPort, getFilePort, generatePublicUrlPort);
        order.verify(checkFileAccessPort).assertCanRead(FILE_ID, 10, false);
        order.verify(getFilePort).getById(FILE_ID);
        order.verify(checkFileAccessPort).assertCanRead(FILE_ID, 1, true);
        order.verify(generatePublicUrlPort).generate(FILE_ID);
    }

    @Test
    @DisplayName("URL firmada denegada → no se genera")
    void generateUrl_denied_doesNotGenerate() {
        doThrow(new AccessDeniedException("no")).when(checkFileAccessPort).assertCanRead(FILE_ID, 10, false);

        assertThatThrownBy(() -> controller.generateUrl(FILE_ID, USER)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(generatePublicUrlPort, getFilePort);
    }

    @Test
    @DisplayName("listar por dueño → filtra según el usuario autenticado")
    void getByOwner_passesRequester() {
        controller.getByOwner("USER", 3, USER);

        verify(getFilesByOwnerPort).getByOwner(3, "USER", 10, false);
        verify(checkFileAccessPort, never()).assertCanRead(any(), any(), anyBoolean());
    }
}
