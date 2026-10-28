package com.capysoft.storage.infrastructure.config;

import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.dto.response.UploadFileResponse;
import com.capysoft.storage.application.port.in.UploadFilePort;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DefaultAvatarInitializer")
class DefaultAvatarInitializerTest {

    private final StoredFileRepository repository = mock(StoredFileRepository.class);
    private final UploadFilePort uploadFilePort = mock(UploadFilePort.class);
    private final StorageClientPort storageClient = mock(StorageClientPort.class);
    private final DefaultAvatarInitializer initializer =
            new DefaultAvatarInitializer(repository, uploadFilePort, storageClient);

    @BeforeEach
    void bucket() {
        ReflectionTestUtils.setField(initializer, "bucket", "tuevento");
    }

    private void existing(String key) {
        when(repository.findByOwnerEntity(0, "DEFAULT_AVATAR"))
                .thenReturn(List.of(StoredFile.builder().storedFileId(1).s3Key(key).build()));
    }

    @Test
    @DisplayName("sin registro sube el avatar como foto de perfil con dueño DEFAULT_AVATAR/0")
    void uploadsWhenMissing() {
        when(repository.findByOwnerEntity(0, "DEFAULT_AVATAR")).thenReturn(List.of());
        when(uploadFilePort.upload(any())).thenReturn(UploadFileResponse.builder().storedFileId(1).build());

        initializer.run(null);

        ArgumentCaptor<UploadFileRequest> request = ArgumentCaptor.forClass(UploadFileRequest.class);
        verify(uploadFilePort).upload(request.capture());
        assertThat(request.getValue().getFileCategoryCode()).isEqualTo("PROFILE_PICTURE");
        assertThat(request.getValue().getOwnerEntityType()).isEqualTo("DEFAULT_AVATAR");
        assertThat(request.getValue().getOwnerEntityId()).isZero();
        assertThat(request.getValue().getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("con registro y objeto no hace nada")
    void keepsExisting() {
        existing("profile_picture/a.jpg");
        when(storageClient.objectExists("tuevento", "profile_picture/a.jpg")).thenReturn(true);

        initializer.run(null);

        verifyNoInteractions(uploadFilePort);
        verify(storageClient, never()).uploadFile(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("con registro pero sin objeto lo vuelve a subir con la misma clave")
    void restoresMissingObject() {
        existing("profile_picture/a.jpg");
        when(storageClient.objectExists("tuevento", "profile_picture/a.jpg")).thenReturn(false);

        initializer.run(null);

        verify(storageClient).uploadFile(eq("tuevento"), eq("profile_picture/a.jpg"), any(), eq("image/jpeg"));
        verifyNoInteractions(uploadFilePort);
    }

    @Test
    @DisplayName("un fallo del almacenamiento no impide arrancar")
    void storageFailureDoesNotStopStartup() {
        when(repository.findByOwnerEntity(0, "DEFAULT_AVATAR")).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
    }
}
