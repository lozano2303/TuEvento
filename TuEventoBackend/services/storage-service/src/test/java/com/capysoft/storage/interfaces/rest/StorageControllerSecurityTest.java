package com.capysoft.storage.interfaces.rest;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.storage.application.dto.response.StoredFileResponse;
import com.capysoft.storage.application.dto.response.UploadFileResponse;
import com.capysoft.storage.application.port.in.DeleteFilePort;
import com.capysoft.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.storage.application.port.in.GetFilePort;
import com.capysoft.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.storage.application.port.in.UploadUserFilePort;
import com.capysoft.common.domain.exception.ForbiddenException;
import com.capysoft.storage.infrastructure.security.SecurityConfig;
import com.capysoft.storage.interfaces.advice.GlobalExceptionHandler;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.time.Instant;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import com.capysoft.common.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests for {@link StorageController} behind the real {@link SecurityConfig}:
 * every storage route requires a login, the principal reaches the use cases as the requester,
 * and access errors keep their code.
 */
@WebMvcTest(StorageController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("StorageController security")
class StorageControllerSecurityTest {

    private static final SecurityUser OWNER = new SecurityUser(42, "owner", "USER");
    private static final SecurityUser OTHER = new SecurityUser(99, "other", "ORGANIZER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");

    @Autowired private MockMvc mockMvc;

    // Sin servidor JWKS en el test: la autenticación se inyecta con el mismo conversor que en producción
    @MockitoBean private JwtDecoder            jwtDecoder;
    @MockitoBean private UploadUserFilePort    uploadUserFilePort;
    @MockitoBean private DeleteFilePort        deleteFilePort;
    @MockitoBean private GetFilePort           getFilePort;
    @MockitoBean private GetFilesByOwnerPort   getFilesByOwnerPort;
    @MockitoBean private GeneratePublicUrlPort generatePublicUrlPort;

    /** Petición autenticada con un JWT de Identidad, convertido igual que en producción. */
    private static RequestPostProcessor as(SecurityUser user) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(user.getAlias())
                .claim("userId", user.getUserId()).claim("role", user.getRole())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return authentication(SecurityConfig.toAuthentication(jwt));
    }

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
        @DisplayName("GET /{id}, /{id}/url and /owner/** → 401 before reaching the use cases")
        void reads_unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/storage/10")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/storage/10/url")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/storage/owner/EVENT/5")).andExpect(status().isUnauthorized());
            verifyNoInteractions(getFilePort, generatePublicUrlPort, getFilesByOwnerPort);
        }
    }

    @Nested
    @DisplayName("Authenticated")
    class Authenticated {

        @Test
        @DisplayName("other user GET /{id} of a public file → 200, use case called with the principal")
        void getFile_public_ok() throws Exception {
            when(getFilePort.getById(eq(10), any())).thenReturn(StoredFileResponse.builder().storedFileId(10).build());

            mockMvc.perform(get("/api/v1/storage/10").with(as(OTHER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.storedFileId").value(10));
            verify(getFilePort).getById(10, StorageRequester.of(99, "ORGANIZER"));
        }

        @Test
        @DisplayName("other user GET /owner/** → listing filtered for the principal")
        void getByOwner_passesPrincipal() throws Exception {
            when(getFilesByOwnerPort.getByOwner(any(), any(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/storage/owner/EVENT/5").with(as(OTHER))).andExpect(status().isOk());
            verify(getFilesByOwnerPort).getByOwner(5, "EVENT", StorageRequester.of(99, "ORGANIZER"));
        }

        @Test
        @DisplayName("other user GET /{id}/url of an organizer document → 403 with STORAGE_ACCESS_DENIED")
        void url_private_forbidden() throws Exception {
            when(generatePublicUrlPort.generate(eq(10), any())).thenThrow(
                    new ForbiddenException("STORAGE_ACCESS_DENIED", "You do not have access to this file"));

            mockMvc.perform(get("/api/v1/storage/10/url").with(as(OTHER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("STORAGE_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("owner POST /upload → requester is the principal; ownerEntity params are ignored")
        void upload_owner_usesPrincipal() throws Exception {
            when(uploadUserFilePort.uploadAsUser(any(), any(), any(), any(), any()))
                    .thenReturn(UploadFileResponse.builder().storedFileId(10).build());

            mockMvc.perform(multipart("/api/v1/storage/upload").file(avatar())
                            .param("categoryCode", "PROFILE_PICTURE")
                            .param("ownerEntityType", "EVENT")
                            .param("ownerEntityId", "777")
                            .with(as(OWNER)))
                    .andExpect(status().isOk());

            verify(uploadUserFilePort).uploadAsUser(eq("PROFILE_PICTURE"), eq("a.jpg"), eq("image/jpeg"),
                    any(byte[].class), eq(StorageRequester.of(42, "USER")));
        }

        @Test
        @DisplayName("owner DELETE /{id} → 200, requester is the principal")
        void delete_owner_ok() throws Exception {
            mockMvc.perform(delete("/api/v1/storage/10").with(as(OWNER))).andExpect(status().isOk());

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

            mockMvc.perform(delete("/api/v1/storage/10").with(as(OTHER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("STORAGE_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("ADMIN GET /{id}/url → 200, requester carries the ADMIN role")
        void url_admin_ok() throws Exception {
            when(generatePublicUrlPort.generate(eq(10), any()))
                    .thenReturn(PublicUrlResponse.builder().storedFileId(10).publicUrl("https://s3/x").build());

            mockMvc.perform(get("/api/v1/storage/10/url").with(as(ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.publicUrl").value("https://s3/x"));
            verify(generatePublicUrlPort).generate(10, StorageRequester.of(1, "ADMIN"));
        }
    }

    @Test
    @DisplayName("GET /{id} with a non-numeric id → 400 INVALID_PARAMETER without echoing the value")
    void typeMismatch_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/storage/abc<script>").with(as(OWNER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'fileId'"));
    }
}
