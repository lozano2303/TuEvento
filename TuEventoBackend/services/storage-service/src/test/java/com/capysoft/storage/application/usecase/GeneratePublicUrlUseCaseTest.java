package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.domain.model.StorageOperationLog;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.ForbiddenException;
import com.capysoft.common.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.capysoft.storage.StorageTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeneratePublicUrlUseCase}: presigned URLs follow the read rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeneratePublicUrlUseCase")
class GeneratePublicUrlUseCaseTest {

    private static final String PRESIGNED = "https://s3.example.com/tuevento/x?X-Amz-Signature=abc";

    @Mock private StoredFileRepository          storedFileRepository;
    @Mock private StorageOperationLogRepository operationLogRepository;
    @Mock private StorageClientPort             storageClient;
    @Mock private ApplicationEventPublisher     eventPublisher;
    @Spy  private StorageAccessPolicy           accessPolicy = new StorageAccessPolicy();

    @InjectMocks
    private GeneratePublicUrlUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "defaultBucket", "tuevento");
    }

    private void givenFile(StoredFile file) {
        when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
    }

    private void givenPresignWorks() {
        when(storageClient.generatePresignedUrl(eq("tuevento"), anyString(), anyInt())).thenReturn(PRESIGNED);
    }

    @Nested
    @DisplayName("Organizer document")
    class PrivateFile {

        @Test
        @DisplayName("anonymous → 401 and no URL is signed")
        void anonymous_unauthorized() {
            givenFile(organizerDocument());

            assertThatThrownBy(() -> useCase.generate(FILE_ID, ANONYMOUS))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasFieldOrPropertyWithValue("code", "STORAGE_AUTHENTICATION_REQUIRED");
            verifyNoInteractions(storageClient);
        }

        @Test
        @DisplayName("requester → signed URL, logged with their user id")
        void owner_allowed() {
            givenFile(organizerDocument());
            givenPresignWorks();

            assertThat(useCase.generate(FILE_ID, OWNER).getPublicUrl()).isEqualTo(PRESIGNED);

            ArgumentCaptor<StorageOperationLog> log = ArgumentCaptor.forClass(StorageOperationLog.class);
            verify(operationLogRepository).save(log.capture());
            assertThat(log.getValue().getPerformedBy()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("other user → 403 and no URL is signed")
        void other_forbidden() {
            givenFile(organizerDocument());

            assertThatThrownBy(() -> useCase.generate(FILE_ID, OTHER))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("code", "STORAGE_ACCESS_DENIED");
            verifyNoInteractions(storageClient);
        }

        @Test
        @DisplayName("ADMIN → signed URL")
        void admin_allowed() {
            givenFile(organizerDocument());
            givenPresignWorks();

            assertThat(useCase.generate(FILE_ID, ADMIN).getPublicUrl()).isEqualTo(PRESIGNED);
        }
    }

    @Test
    @DisplayName("public file, anonymous → signed URL, logged as system user 0")
    void publicFile_anonymous_allowed() {
        givenFile(profilePicture());
        givenPresignWorks();

        assertThat(useCase.generate(FILE_ID, ANONYMOUS).getPublicUrl()).isEqualTo(PRESIGNED);

        ArgumentCaptor<StorageOperationLog> log = ArgumentCaptor.forClass(StorageOperationLog.class);
        verify(operationLogRepository).save(log.capture());
        assertThat(log.getValue().getPerformedBy()).isZero();
    }
}
