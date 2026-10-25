package com.capysoft.tuevento.modules.storage.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.port.out.CurrentRolePort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.DeleteFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadUserFilePort;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.UnauthorizedException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityConfig;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for {@link StorageController} behind the real {@link SecurityConfig}:
 * which routes are public, which require a login, and how access errors are rendered.
 */
@WebMvcTest(StorageController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
@DisplayName("StorageController security")
class StorageControllerSecurityTest {

    private static final SecurityUser OWNER = new SecurityUser(42, "owner", "USER");
    private static final SecurityUser OTHER = new SecurityUser(99, "other", "ORGANIZER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TokenGeneratorPort    tokenGenerator;
    @MockitoBean private CurrentRolePort       currentRolePort;
    @MockitoBean private UploadUserFilePort    uploadUserFilePort;
    @MockitoBean private DeleteFilePort        deleteFilePort;
    @MockitoBean private GetFilePort           getFilePort;
    @MockitoBean private GetFilesByOwnerPort   getFilesByOwnerPort;
    @MockitoBean private GeneratePublicUrlPort generatePublicUrlPort;

    private static MockMultipartFile avatar() {
        return new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    @Nested
    @DisplayName("Anonymous")
    class Anonymous {

        @Test
        @DisplayName("POST /upload → 401 before reaching the use case")
        void upload_unauthorized() throws Exception {
            mockMvc.perform(multipart("/api/v1/storage/upload").file(avatar()).param("categoryCode", "PROFILE_PICTURE"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(uploadUserFilePort);
        }

        @Test
        @DisplayName("DELETE /{id} → 401 before reaching the use case")
        void delete_unauthorized() throws Exception {
            mockMvc.perform(delete("/api/v1/storage/10")).andExpect(status().isUnauthorized());
            verifyNoInteractions(deleteFilePort);
        }

        @Test
        @DisplayName("GET /{id} of a public file → 200, use case called as anonymous")
        void getFile_public_ok() throws Exception {
            when(getFilePort.getById(eq(10), any())).thenReturn(StoredFileResponse.builder().storedFileId(10).build());

            mockMvc.perform(get("/api/v1/storage/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.storedFileId").value(10));
            verify(getFilePort).getById(10, StorageRequester.anonymous());
        }

        @Test
        @DisplayName("GET /owner/{type}/{id} → 200, use case filters as anonymous")
        void getByOwner_ok() throws Exception {
            when(getFilesByOwnerPort.getByOwner(any(), any(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/storage/owner/EVENT/5")).andExpect(status().isOk());
            verify(getFilesByOwnerPort).getByOwner(5, "EVENT", StorageRequester.anonymous());
        }

        @Test
        @DisplayName("GET /{id}/url of an organizer document → 401 with STORAGE_AUTHENTICATION_REQUIRED")
        void url_private_unauthorized() throws Exception {
            when(generatePublicUrlPort.generate(eq(10), any())).thenThrow(new UnauthorizedException(
                    "STORAGE_AUTHENTICATION_REQUIRED", "Authentication is required to access this file"));

            mockMvc.perform(get("/api/v1/storage/10/url"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("STORAGE_AUTHENTICATION_REQUIRED"));
        }
    }

    @Nested
    @DisplayName("Authenticated")
    class Authenticated {

        @Test
        @DisplayName("owner POST /upload → requester is the principal; ownerEntity params are ignored")
        void upload_owner_usesPrincipal() throws Exception {
            when(uploadUserFilePort.uploadAsUser(any(), any(), any(), any(), any()))
                    .thenReturn(UploadFileResponse.builder().storedFileId(10).build());

            mockMvc.perform(multipart("/api/v1/storage/upload").file(avatar())
                            .param("categoryCode", "PROFILE_PICTURE")
                            .param("ownerEntityType", "EVENT")
                            .param("ownerEntityId", "777")
                            .with(user(OWNER)))
                    .andExpect(status().isOk());

            verify(uploadUserFilePort).uploadAsUser(eq("PROFILE_PICTURE"), eq("a.jpg"), eq("image/jpeg"),
                    any(byte[].class), eq(StorageRequester.of(42, "USER")));
        }

        @Test
        @DisplayName("owner DELETE /{id} → 200, requester is the principal")
        void delete_owner_ok() throws Exception {
            mockMvc.perform(delete("/api/v1/storage/10").with(user(OWNER))).andExpect(status().isOk());

            ArgumentCaptor<DeleteFileRequest> request = ArgumentCaptor.forClass(DeleteFileRequest.class);
            verify(deleteFilePort).delete(request.capture());
            assertThat(request.getValue().getStoredFileId()).isEqualTo(10);
            assertThat(request.getValue().getRequester()).isEqualTo(StorageRequester.of(42, "USER"));
        }

        @Test
        @DisplayName("other user DELETE /{id} → 403 with STORAGE_ACCESS_DENIED")
        void delete_other_forbidden() throws Exception {
            doThrow(new ForbiddenException("STORAGE_ACCESS_DENIED", "You do not have access to this file"))
                    .when(deleteFilePort).delete(any());

            mockMvc.perform(delete("/api/v1/storage/10").with(user(OTHER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("STORAGE_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("ADMIN GET /{id}/url → 200, requester carries the ADMIN role")
        void url_admin_ok() throws Exception {
            when(generatePublicUrlPort.generate(eq(10), any()))
                    .thenReturn(PublicUrlResponse.builder().storedFileId(10).publicUrl("https://s3/x").build());

            mockMvc.perform(get("/api/v1/storage/10/url").with(user(ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.publicUrl").value("https://s3/x"));
            verify(generatePublicUrlPort).generate(10, StorageRequester.of(1, "ADMIN"));
        }
    }

    @Test
    @DisplayName("GET /{id} with a non-numeric id → 400 INVALID_PARAMETER without echoing the value")
    void typeMismatch_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/storage/abc<script>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'fileId'"));
    }
}
