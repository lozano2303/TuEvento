package com.capysoft.tuevento.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.shared.application.storage.FileStorage;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;

@DisplayName("ProfileDataInitializer (default avatar)")
class ProfileDataInitializerTest {

    private final FileStorage files = mock(FileStorage.class);
    private final IdentityServiceClient identity = mock(IdentityServiceClient.class);
    private final ProfileDataInitializer initializer = new ProfileDataInitializer(files, identity);

    @Test
    @DisplayName("the avatar uploaded by storage-service is assigned to the profiles without one through identity-service")
    void assignsExistingAvatar() {
        when(files.findIdsByOwner("DEFAULT_AVATAR", 0)).thenReturn(List.of(1));

        initializer.run(new DefaultApplicationArguments());

        verify(identity).assignDefaultAvatar(1);
    }

    @Test
    @DisplayName("without an uploaded avatar nothing is assigned")
    void noAvatarYet() {
        when(files.findIdsByOwner("DEFAULT_AVATAR", 0)).thenReturn(List.of());

        initializer.run(new DefaultApplicationArguments());

        verifyNoInteractions(identity);
    }

    @Test
    @DisplayName("storage-service or identity-service down at startup does not stop the monolith from starting")
    void servicesDownDoNotBreakStartup() {
        when(files.findIdsByOwner("DEFAULT_AVATAR", 0))
                .thenThrow(new ServiceUnavailableException("STORAGE_UNAVAILABLE", "down"));
        assertThatCode(() -> initializer.run(new DefaultApplicationArguments())).doesNotThrowAnyException();

        doReturn(List.of(1)).when(files).findIdsByOwner("DEFAULT_AVATAR", 0);
        when(identity.assignDefaultAvatar(1)).thenThrow(new ServiceUnavailableException("IDENTITY_UNAVAILABLE", "down"));
        assertThatCode(() -> initializer.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
