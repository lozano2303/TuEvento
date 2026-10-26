package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.storage.application.dto.request.StorageRequester;
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
 * Unit tests for {@link DeleteFileUseCase}: owner or ADMIN; organizer documents only ADMIN.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteFileUseCase")
class DeleteFileUseCaseTest {

    @Mock private StoredFileRepository          storedFileRepository;
    @Mock private StorageOperationLogRepository operationLogRepository;
    @Mock private StorageClientPort             storageClient;
    @Mock private ApplicationEventPublisher     eventPublisher;
    @Spy  private StorageAccessPolicy           accessPolicy = new StorageAccessPolicy();

    @InjectMocks
    private DeleteFileUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "defaultBucket", "tuevento");
    }

    private void givenFile(StoredFile file) {
        when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
    }

    private void delete(StorageRequester requester) {
        useCase.delete(DeleteFileRequest.builder().storedFileId(FILE_ID).requester(requester).build());
    }

    private void assertNothingDeleted() {
        verify(storedFileRepository, never()).softDelete(any());
        verifyNoInteractions(storageClient);
    }

    @Nested
    @DisplayName("Public file")
    class PublicFile {

        @Test
        @DisplayName("anonymous → 401, nothing deleted")
        void anonymous_unauthorized() {
            givenFile(profilePicture());
            assertThatThrownBy(() -> delete(ANONYMOUS)).isInstanceOf(UnauthorizedException.class);
            assertNothingDeleted();
        }

        @Test
        @DisplayName("owner → soft-deletes, removes the object and logs the owner")
        void owner_deletes() {
            givenFile(profilePicture());

            delete(OWNER);

            verify(storedFileRepository).softDelete(FILE_ID);
            verify(storageClient).deleteFile("tuevento", "profile_picture/avatar.jpg");
            ArgumentCaptor<StorageOperationLog> log = ArgumentCaptor.forClass(StorageOperationLog.class);
            verify(operationLogRepository).save(log.capture());
            assertThat(log.getValue().getPerformedBy()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("other user → 403 STORAGE_ACCESS_DENIED, nothing deleted")
        void other_forbidden() {
            givenFile(eventMedia());
            assertThatThrownBy(() -> delete(OTHER))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("code", "STORAGE_ACCESS_DENIED");
            assertNothingDeleted();
        }

        @Test
        @DisplayName("ADMIN → deletes")
        void admin_deletes() {
            givenFile(eventMedia());
            delete(ADMIN);
            verify(storedFileRepository).softDelete(FILE_ID);
        }
    }

    @Nested
    @DisplayName("Organizer document")
    class PrivateFile {

        @Test
        @DisplayName("anonymous → 401")
        void anonymous_unauthorized() {
            givenFile(organizerDocument());
            assertThatThrownBy(() -> delete(ANONYMOUS)).isInstanceOf(UnauthorizedException.class);
            assertNothingDeleted();
        }

        @Test
        @DisplayName("requester → 403: only an ADMIN may delete it")
        void owner_forbidden() {
            givenFile(organizerDocument());
            assertThatThrownBy(() -> delete(OWNER))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("code", "STORAGE_ACCESS_DENIED");
            assertNothingDeleted();
        }

        @Test
        @DisplayName("other user → 403")
        void other_forbidden() {
            givenFile(organizerDocument());
            assertThatThrownBy(() -> delete(OTHER)).isInstanceOf(ForbiddenException.class);
            assertNothingDeleted();
        }

        @Test
        @DisplayName("ADMIN → deletes")
        void admin_deletes() {
            givenFile(organizerDocument());
            delete(ADMIN);
            verify(storedFileRepository).softDelete(FILE_ID);
        }
    }

    @Test
    @DisplayName("storage failure is logged as FAILED with the message truncated to 500 chars")
    void storageFailure_truncatesLoggedError() {
        givenFile(profilePicture());
        doThrow(new RuntimeException("x".repeat(800))).when(storageClient).deleteFile(anyString(), anyString());

        delete(OWNER);

        ArgumentCaptor<StorageOperationLog> log = ArgumentCaptor.forClass(StorageOperationLog.class);
        verify(operationLogRepository).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(log.getValue().getErrorMessage()).hasSize(500);
    }
}
