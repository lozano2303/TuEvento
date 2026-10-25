package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.out.StorageClientPort;
import com.capysoft.tuevento.modules.storage.application.service.StorageAccessPolicy;
import com.capysoft.tuevento.modules.storage.application.service.StoredFileResponseFactory;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.capysoft.tuevento.modules.storage.StorageTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetFileUseCase}: read access rules and the public URL derived on read.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetFileUseCase")
class GetFileUseCaseTest {

    private static final String PUBLIC_HOST = "https://s3.example.com";

    @Mock private StoredFileRepository storedFileRepository;
    @Mock private StorageClientPort    storageClient;

    private GetFileUseCase useCase;

    @BeforeEach
    void setUp() {
        StorageAccessPolicy policy = new StorageAccessPolicy();
        StoredFileResponseFactory factory = new StoredFileResponseFactory(storageClient, policy);
        ReflectionTestUtils.setField(factory, "defaultBucket", "tuevento");
        useCase = new GetFileUseCase(storedFileRepository, policy, factory);
    }

    private void givenFile(StoredFile file) {
        when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
    }

    @Nested
    @DisplayName("Public file")
    class PublicFile {

        @Test
        @DisplayName("anonymous → returns metadata with a URL built from the public host, not the stored one")
        void anonymous_returnsPublicHostUrl() {
            givenFile(eventMedia());
            when(storageClient.generatePublicUrl("tuevento", "event_media/cover.jpg"))
                    .thenReturn(PUBLIC_HOST + "/tuevento/event_media/cover.jpg");

            StoredFileResponse response = useCase.getById(FILE_ID, ANONYMOUS);

            assertThat(response.getPublicUrl()).isEqualTo(PUBLIC_HOST + "/tuevento/event_media/cover.jpg");
            assertThat(response.getPublicUrl()).doesNotContain("minio:9000");
        }
    }

    @Nested
    @DisplayName("Organizer document")
    class PrivateFile {

        @Test
        @DisplayName("anonymous → 401")
        void anonymous_unauthorized() {
            givenFile(organizerDocument());
            assertThatThrownBy(() -> useCase.getById(FILE_ID, ANONYMOUS))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("requester → metadata without public URL")
        void owner_noPublicUrl() {
            givenFile(organizerDocument());

            StoredFileResponse response = useCase.getById(FILE_ID, OWNER);

            assertThat(response.getStoredFileId()).isEqualTo(FILE_ID);
            assertThat(response.getPublicUrl()).isNull();
            verify(storageClient, never()).generatePublicUrl(anyString(), anyString());
        }

        @Test
        @DisplayName("other user → 403 STORAGE_ACCESS_DENIED")
        void other_forbidden() {
            givenFile(organizerDocument());
            assertThatThrownBy(() -> useCase.getById(FILE_ID, OTHER))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("code", "STORAGE_ACCESS_DENIED");
        }

        @Test
        @DisplayName("ADMIN → allowed")
        void admin_allowed() {
            givenFile(organizerDocument());
            assertThat(useCase.getById(FILE_ID, ADMIN).getStoredFileId()).isEqualTo(FILE_ID);
        }
    }

    @Test
    @DisplayName("deleted file → FILE_NOT_FOUND")
    void deleted_notFound() {
        StoredFile file = profilePicture();
        file.setDeleted(true);
        givenFile(file);

        assertThatThrownBy(() -> useCase.getById(FILE_ID, ADMIN))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "FILE_NOT_FOUND");
    }
}
