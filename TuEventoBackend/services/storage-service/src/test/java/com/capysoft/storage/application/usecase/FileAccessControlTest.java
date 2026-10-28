package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.application.service.StoredFileResponseFactory;
import com.capysoft.storage.domain.model.FileCategory;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.ForbiddenException;
import com.capysoft.common.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * End-to-end access scenarios for files exposed by the storage API, run through the use cases
 * and the real {@link StorageAccessPolicy}.
 *
 * <ul>
 *   <li>Public categories (profile picture, event media): any authenticated user can read them</li>
 *   <li>Private categories (organizer documents): only the uploader or an admin</li>
 *   <li>Delete: the uploader or an admin; organizer documents only an admin</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Storage file access control")
class FileAccessControlTest {

    private static final Integer UPLOADER_ID = 10;
    private static final Integer OTHER_USER_ID = 20;
    private static final Integer FILE_ID = 5;

    private static final StorageRequester UPLOADER = StorageRequester.of(UPLOADER_ID, "USER");
    private static final StorageRequester OTHER    = StorageRequester.of(OTHER_USER_ID, "USER");
    private static final StorageRequester ADMIN    = StorageRequester.of(OTHER_USER_ID, "ADMIN");

    @Mock private StoredFileRepository          storedFileRepository;
    @Mock private StorageOperationLogRepository operationLogRepository;
    @Mock private StorageClientPort             storageClient;
    @Mock private ApplicationEventPublisher     eventPublisher;

    private GetFileUseCase         getFile;
    private GetFilesByOwnerUseCase getFilesByOwner;
    private DeleteFileUseCase      deleteFile;

    @BeforeEach
    void setUp() {
        StorageAccessPolicy policy = new StorageAccessPolicy();
        StoredFileResponseFactory factory = new StoredFileResponseFactory(storageClient, policy);
        ReflectionTestUtils.setField(factory, "defaultBucket", "tuevento");
        getFile = new GetFileUseCase(storedFileRepository, policy, factory);
        getFilesByOwner = new GetFilesByOwnerUseCase(storedFileRepository, policy, factory);
        deleteFile = new DeleteFileUseCase(storedFileRepository, operationLogRepository, storageClient,
                eventPublisher, policy);
        ReflectionTestUtils.setField(deleteFile, "defaultBucket", "tuevento");
    }

    private static StoredFile file(Integer id, boolean publicCategory) {
        return StoredFile.builder()
                .storedFileId(id)
                .uploadedBy(UPLOADER_ID)
                .s3Key("k" + id)
                .deleted(false)
                .fileCategory(FileCategory.builder()
                        .code(publicCategory ? "PROFILE_PICTURE" : "ORGANIZER_DOCUMENT")
                        .isPublic(publicCategory)
                        .build())
                .build();
    }

    private void givenFile(boolean publicCategory) {
        when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file(FILE_ID, publicCategory)));
    }

    private void delete(StorageRequester requester) {
        deleteFile.delete(DeleteFileRequest.builder().storedFileId(FILE_ID).requester(requester).build());
    }

    @Nested
    @DisplayName("Read (file info)")
    class Read {

        @Test
        @DisplayName("Private document → another user is denied")
        void privateFile_otherUser_isDenied() {
            givenFile(false);

            assertThatThrownBy(() -> getFile.getById(FILE_ID, OTHER)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Private document → the uploader and an admin are allowed")
        void privateFile_uploaderOrAdmin_isAllowed() {
            givenFile(false);

            assertThatCode(() -> getFile.getById(FILE_ID, UPLOADER)).doesNotThrowAnyException();
            assertThatCode(() -> getFile.getById(FILE_ID, ADMIN)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Public category → any authenticated user is allowed")
        void publicFile_anyUser_isAllowed() {
            givenFile(true);

            assertThatCode(() -> getFile.getById(FILE_ID, OTHER)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Missing file → NotFoundException")
        void missingFile_isNotFound() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> getFile.getById(FILE_ID, UPLOADER)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Public file of someone else → denied (public read does not mean public delete)")
        void publicFile_otherUser_cannotDelete() {
            givenFile(true);

            assertThatThrownBy(() -> delete(OTHER)).isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Public file → uploader and admin are allowed")
        void publicFile_uploaderOrAdmin_canDelete() {
            givenFile(true);

            assertThatCode(() -> delete(UPLOADER)).doesNotThrowAnyException();
            assertThatCode(() -> delete(ADMIN)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Organizer document → only an admin; the uploader is denied so the petition keeps its evidence")
        void organizerDocument_onlyAdmin_canDelete() {
            givenFile(false);

            assertThatThrownBy(() -> delete(UPLOADER)).isInstanceOf(ForbiddenException.class);
            assertThatCode(() -> delete(ADMIN)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("List by owner → hides private files the requester cannot see")
    void listByOwner_hidesPrivateFilesOfOthers() {
        when(storedFileRepository.findByOwnerEntity(1, "USER"))
                .thenReturn(List.of(file(1, true), file(2, false)));

        assertThat(getFilesByOwner.getByOwner(1, "USER", OTHER))
                .extracting("storedFileId").containsExactly(1);
        assertThat(getFilesByOwner.getByOwner(1, "USER", UPLOADER))
                .extracting("storedFileId").containsExactly(1, 2);
    }
}
