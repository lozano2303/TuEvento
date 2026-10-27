package com.capysoft.storage.interfaces.rest;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.port.in.DeleteFilePort;
import com.capysoft.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.storage.application.port.in.GetFilePort;
import com.capysoft.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.storage.application.port.in.UploadUserFilePort;
import com.capysoft.common.domain.exception.ForbiddenException;
import com.capysoft.common.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * The controller only maps HTTP to the use cases: it always passes the authenticated user as the
 * requester (never an id sent by the client) and lets the use cases' access errors propagate.
 * The access rules themselves are covered by StorageAccessPolicyTest and the use case tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StorageController")
class StorageControllerTest {

    private static final Integer FILE_ID = 5;
    private static final SecurityUser USER  = new SecurityUser(10, "user", "USER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");

    private static final StorageRequester USER_REQUESTER  = StorageRequester.of(10, "USER");
    private static final StorageRequester ADMIN_REQUESTER = StorageRequester.of(1, "ADMIN");

    @Mock private UploadUserFilePort    uploadUserFilePort;
    @Mock private DeleteFilePort        deleteFilePort;
    @Mock private GetFilePort           getFilePort;
    @Mock private GetFilesByOwnerPort   getFilesByOwnerPort;
    @Mock private GeneratePublicUrlPort generatePublicUrlPort;
    @InjectMocks private StorageController controller;

    @Test
    @DisplayName("upload → the file is uploaded as the authenticated user")
    void upload_usesAuthenticatedUser() throws Exception {
        controller.upload(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1}), "PROFILE_PICTURE", USER);

        verify(uploadUserFilePort).uploadAsUser(eq("PROFILE_PICTURE"), eq("a.jpg"), eq("image/jpeg"),
                any(byte[].class), eq(USER_REQUESTER));
    }

    @Test
    @DisplayName("delete → the authenticated user is the requester")
    void delete_passesRequester() {
        controller.delete(FILE_ID, USER);

        ArgumentCaptor<DeleteFileRequest> request = ArgumentCaptor.forClass(DeleteFileRequest.class);
        verify(deleteFilePort).delete(request.capture());
        assertThat(request.getValue().getStoredFileId()).isEqualTo(FILE_ID);
        assertThat(request.getValue().getRequester()).isEqualTo(USER_REQUESTER);
    }

    @Test
    @DisplayName("delete denied by the use case → the error propagates")
    void delete_denied_propagates() {
        doThrow(new ForbiddenException("STORAGE_ACCESS_DENIED", "no")).when(deleteFilePort).delete(any());

        assertThatThrownBy(() -> controller.delete(FILE_ID, USER))
                .isInstanceOf(ForbiddenException.class)
                .hasFieldOrPropertyWithValue("code", "STORAGE_ACCESS_DENIED");
    }

    @Test
    @DisplayName("info and presigned URL → the requester carries the role (ADMIN included)")
    void read_passesRequester() {
        controller.getFile(FILE_ID, USER);
        controller.generateUrl(FILE_ID, ADMIN);

        verify(getFilePort).getById(FILE_ID, USER_REQUESTER);
        verify(generatePublicUrlPort).generate(FILE_ID, ADMIN_REQUESTER);
    }

    @Test
    @DisplayName("list by owner → filtered for the authenticated user")
    void getByOwner_passesRequester() {
        controller.getByOwner("USER", 3, USER);

        verify(getFilesByOwnerPort).getByOwner(3, "USER", USER_REQUESTER);
    }
}
