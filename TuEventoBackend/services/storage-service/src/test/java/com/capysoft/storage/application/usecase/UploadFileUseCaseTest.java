package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.ImageModerationService;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.domain.model.FileCategory;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.FileCategoryRepository;
import com.capysoft.storage.domain.repository.StorageOperationLogRepository;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import com.capysoft.common.domain.exception.BusinessException;
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
 * Unit tests for {@link UploadFileUseCase}: REST upload rules and error messages that never
 * leak storage SDK details.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UploadFileUseCase")
class UploadFileUseCaseTest {

    private static final byte[] CONTENT = {1, 2, 3};

    @Mock private FileCategoryRepository        fileCategoryRepository;
    @Mock private StoredFileRepository          storedFileRepository;
    @Mock private StorageOperationLogRepository operationLogRepository;
    @Mock private StorageClientPort             storageClient;
    @Mock private ApplicationEventPublisher     eventPublisher;
    @Mock private ImageModerationService        moderationService;
    @Spy  private StorageAccessPolicy           accessPolicy = new StorageAccessPolicy();

    @InjectMocks
    private UploadFileUseCase useCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "defaultBucket", "tuevento");
    }

    private void givenProfilePictureCategory() {
        when(fileCategoryRepository.findByCode("PROFILE_PICTURE")).thenReturn(Optional.of(FileCategory.builder()
                .code("PROFILE_PICTURE").isPublic(true).allowedExtensions("jpg,png").maxSizeMb(2).build()));
    }

    private void givenSaveReturnsInput() {
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(inv -> {
            StoredFile f = inv.getArgument(0);
            f.setStoredFileId(FILE_ID);
            return f;
        });
    }

    @Nested
    @DisplayName("uploadAsUser (REST)")
    class UploadAsUser {

        @Test
        @DisplayName("anonymous → 401, nothing stored")
        void anonymous_unauthorized() {
            assertThatThrownBy(() -> useCase.uploadAsUser("PROFILE_PICTURE", "a.jpg", "image/jpeg", CONTENT, ANONYMOUS))
                    .isInstanceOf(UnauthorizedException.class);
            verifyNoInteractions(storageClient, storedFileRepository);
        }

        @Test
        @DisplayName("ORGANIZER_DOCUMENT → 403 STORAGE_CATEGORY_NOT_ALLOWED")
        void organizerDocument_forbidden() {
            assertThatThrownBy(() -> useCase.uploadAsUser("ORGANIZER_DOCUMENT", "a.pdf", "application/pdf", CONTENT, OWNER))
                    .isInstanceOf(ForbiddenException.class)
                    .hasFieldOrPropertyWithValue("code", "STORAGE_CATEGORY_NOT_ALLOWED");
            verifyNoInteractions(storageClient, storedFileRepository);
        }

        @Test
        @DisplayName("PROFILE_PICTURE → stored with the authenticated user as uploader and no owner entity")
        void profilePicture_uploaderIsAuthenticatedUser() {
            givenProfilePictureCategory();
            givenSaveReturnsInput();
            when(storageClient.generatePublicUrl(eq("tuevento"), anyString())).thenReturn("https://s3.example.com/x");

            useCase.uploadAsUser("PROFILE_PICTURE", "a.jpg", "image/jpeg", CONTENT, OTHER);

            ArgumentCaptor<StoredFile> saved = ArgumentCaptor.forClass(StoredFile.class);
            verify(storedFileRepository).save(saved.capture());
            assertThat(saved.getValue().getUploadedBy()).isEqualTo(OTHER_ID);
            assertThat(saved.getValue().getOwnerEntityId()).isNull();
            assertThat(saved.getValue().getOwnerEntityType()).isNull();
            assertThat(saved.getValue().getPublicUrl()).isEqualTo("https://s3.example.com/x");
        }
    }

    @Nested
    @DisplayName("Error messages")
    class ErrorMessages {

        private UploadFileRequest request() {
            return UploadFileRequest.builder()
                    .fileCategoryCode("PROFILE_PICTURE").uploadedBy(OWNER_ID)
                    .originalFilename("a.jpg").contentType("image/jpeg").content(CONTENT).build();
        }

        @Test
        @DisplayName("unexpected failure → UPLOAD_FAILED with a fixed message")
        void unexpectedFailure_fixedMessage() {
            givenProfilePictureCategory();
            when(storageClient.uploadFile(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Connection refused: minio:9000 secret=abc"));

            assertThatThrownBy(() -> useCase.upload(request()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "UPLOAD_FAILED")
                    .hasMessage("Failed to upload file");
        }

        @Test
        @DisplayName("storage client BusinessException → propagated with its own code")
        void storageClientError_keepsCode() {
            givenProfilePictureCategory();
            when(storageClient.uploadFile(any(), any(), any(), any()))
                    .thenThrow(new BusinessException("S3_UPLOAD_FAILED", "Failed to upload file"));

            assertThatThrownBy(() -> useCase.upload(request()))
                    .hasFieldOrPropertyWithValue("code", "S3_UPLOAD_FAILED")
                    .hasMessage("Failed to upload file");
        }
    }
}
