package com.capysoft.storage.interfaces.rest;

import com.capysoft.common.infrastructure.security.InternalApiKeyFilter;
import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.storage.application.dto.response.StoredFileResponse;
import com.capysoft.storage.application.dto.response.UploadFileResponse;
import com.capysoft.storage.application.port.in.DeleteFilePort;
import com.capysoft.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.storage.application.port.in.GetFilePort;
import com.capysoft.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.storage.application.port.in.UploadFilePort;
import com.capysoft.storage.application.port.in.UploadUserFilePort;
import com.capysoft.storage.infrastructure.security.SecurityConfig;
import com.capysoft.storage.interfaces.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API interna de archivos detrás del {@link SecurityConfig} real: solo con la clave entre servicios. */
@WebMvcTest({InternalStorageController.class, StorageController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.internal.api-key=test-key")
@DisplayName("InternalStorageController")
class InternalStorageControllerTest {

    private static final String KEY = InternalApiKeyFilter.HEADER;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder            jwtDecoder;
    @MockitoBean private UploadFilePort        uploadFilePort;
    @MockitoBean private UploadUserFilePort    uploadUserFilePort;
    @MockitoBean private DeleteFilePort        deleteFilePort;
    @MockitoBean private GetFilePort           getFilePort;
    @MockitoBean private GetFilesByOwnerPort   getFilesByOwnerPort;
    @MockitoBean private GeneratePublicUrlPort generatePublicUrlPort;

    @Test
    @DisplayName("sin clave o con una clave equivocada responde 401 y no toca los casos de uso")
    void rejectsWithoutKey() throws Exception {
        mockMvc.perform(get("/internal/files/7"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INTERNAL_AUTH_REQUIRED"));
        mockMvc.perform(get("/internal/files/7").header(KEY, "wrong"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(getFilePort);
    }

    @Test
    @DisplayName("la subida de confianza recibe categoría, autor y dueño de quien llama")
    void uploads() throws Exception {
        when(uploadFilePort.upload(any())).thenReturn(UploadFileResponse.builder()
                .storedFileId(5).publicUrl("http://files/event.jpg").build());

        mockMvc.perform(multipart("/internal/files")
                        .file(new MockMultipartFile("file", "event.jpg", "image/jpeg", new byte[]{1, 2}))
                        .param("categoryCode", "EVENT_MEDIA").param("uploadedBy", "42")
                        .param("ownerEntityType", "EVENT").param("ownerEntityId", "9")
                        .header(KEY, "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storedFileId").value(5))
                .andExpect(jsonPath("$.data.publicUrl").value("http://files/event.jpg"));

        ArgumentCaptor<UploadFileRequest> request = ArgumentCaptor.forClass(UploadFileRequest.class);
        verify(uploadFilePort).upload(request.capture());
        assertThat(request.getValue().getFileCategoryCode()).isEqualTo("EVENT_MEDIA");
        assertThat(request.getValue().getUploadedBy()).isEqualTo(42);
        assertThat(request.getValue().getOwnerEntityType()).isEqualTo("EVENT");
        assertThat(request.getValue().getOwnerEntityId()).isEqualTo(9);
        assertThat(request.getValue().getOriginalFilename()).isEqualTo("event.jpg");
        assertThat(request.getValue().getContent()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("las lecturas aplican la política con el usuario en cuyo nombre se pregunta")
    void readsAsRequester() throws Exception {
        when(generatePublicUrlPort.generate(eq(3), any()))
                .thenReturn(PublicUrlResponse.builder().storedFileId(3).publicUrl("http://files/doc.pdf").build());

        mockMvc.perform(get("/internal/files/3/url").param("requesterUserId", "8").param("requesterRole", "ADMIN")
                        .header(KEY, "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicUrl").value("http://files/doc.pdf"));

        verify(generatePublicUrlPort).generate(3, StorageRequester.of(8, "ADMIN"));
    }

    @Test
    @DisplayName("sin usuario se pregunta como anónimo (solo categorías públicas)")
    void readsAnonymously() throws Exception {
        when(getFilePort.getById(eq(7), any())).thenReturn(StoredFileResponse.builder()
                .storedFileId(7).publicUrl("http://files/avatar.jpg").build());
        when(getFilesByOwnerPort.getByOwner(eq(0), eq("DEFAULT_AVATAR"), any()))
                .thenReturn(List.of(StoredFileResponse.builder().storedFileId(1).build()));

        mockMvc.perform(get("/internal/files/7").header(KEY, "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicUrl").value("http://files/avatar.jpg"));
        mockMvc.perform(get("/internal/files/owner/DEFAULT_AVATAR/0").header(KEY, "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].storedFileId").value(1));

        verify(getFilePort).getById(7, StorageRequester.anonymous());
        verify(getFilesByOwnerPort).getByOwner(0, "DEFAULT_AVATAR", StorageRequester.anonymous());
    }
}
