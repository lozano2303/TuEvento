package com.capysoft.tuevento.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;

@DisplayName("ProfileDataInitializer (default avatar)")
class ProfileDataInitializerTest {

    private final StoredFileRepository files = mock(StoredFileRepository.class);
    private final UploadFilePort upload = mock(UploadFilePort.class);
    private final IdentityServiceClient identity = mock(IdentityServiceClient.class);
    private final StorageClientPort storage = mock(StorageClientPort.class);
    private final ProfileDataInitializer initializer = new ProfileDataInitializer(files, upload, identity, storage);

    private void existingAvatar() {
        ReflectionTestUtils.setField(initializer, "defaultBucket", "tuevento");
        when(files.findByOwnerEntity(0, "DEFAULT_AVATAR"))
                .thenReturn(List.of(StoredFile.builder().storedFileId(1).s3Key("avatars/default.jpg").build()));
        when(storage.objectExists("tuevento", "avatars/default.jpg")).thenReturn(true);
    }

    @Test
    @DisplayName("an avatar already in storage is assigned to the profiles without one through identity-service")
    void assignsExistingAvatar() {
        existingAvatar();

        initializer.run(new DefaultApplicationArguments());

        verify(identity).assignDefaultAvatar(1);
        verifyNoInteractions(upload);
    }

    @Test
    @DisplayName("identity-service down at startup does not stop the monolith from starting")
    void identityDownDoesNotBreakStartup() {
        existingAvatar();
        when(identity.assignDefaultAvatar(1)).thenThrow(new ServiceUnavailableException("IDENTITY_UNAVAILABLE", "down"));

        assertThatCode(() -> initializer.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
