package com.capysoft.tuevento.shared.infrastructure.security;

import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenVerifier;
import com.capysoft.tuevento.modules.storage.application.port.in.DeleteFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadUserFilePort;
import com.capysoft.tuevento.modules.storage.interfaces.rest.StorageController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con la cadena de seguridad real: storage ya no es público.
 */
@WebMvcTest(controllers = StorageController.class, properties = "app.cors.allowed-origins=http://localhost:5173")
@Import(SecurityConfig.class)
@DisplayName("Public endpoints security")
class PublicEndpointsSecurityTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private AccessTokenVerifier accessTokenVerifier;
    @MockitoBean private UploadUserFilePort uploadUserFilePort;
    @MockitoBean private DeleteFilePort deleteFilePort;
    @MockitoBean private GetFilePort getFilePort;
    @MockitoBean private GetFilesByOwnerPort getFilesByOwnerPort;
    @MockitoBean private GeneratePublicUrlPort generatePublicUrlPort;

    @Test
    @DisplayName("Storage without a session → 401 for upload, delete, info, owner list and presigned URL")
    void storage_withoutSession_isUnauthorized() throws Exception {
        mvc.perform(multipart("/api/v1/storage/upload")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}))
                        .param("categoryCode", "ORGANIZER_DOCUMENT"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/storage/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/storage/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/storage/5/url")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/storage/owner/USER/1")).andExpect(status().isUnauthorized());

        verifyNoInteractions(uploadUserFilePort, deleteFilePort, getFilePort, generatePublicUrlPort, getFilesByOwnerPort);
    }
}
