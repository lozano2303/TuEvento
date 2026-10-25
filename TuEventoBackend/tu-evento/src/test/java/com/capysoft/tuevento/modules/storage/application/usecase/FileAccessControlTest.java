package com.capysoft.tuevento.modules.storage.application.usecase;

import com.capysoft.tuevento.modules.storage.domain.model.FileCategory;
import com.capysoft.tuevento.modules.storage.domain.model.StoredFile;
import com.capysoft.tuevento.modules.storage.domain.repository.StoredFileRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Control de acceso a archivos expuestos por la API de storage.
 *
 * <ul>
 *   <li>Categorías públicas (foto de perfil, media de eventos): cualquier usuario autenticado las lee</li>
 *   <li>Categorías privadas (documentos de organizador): solo quien subió el archivo o un admin</li>
 *   <li>Borrar: solo quien subió el archivo o un admin</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Storage file access control")
class FileAccessControlTest {

    private static final Integer UPLOADER_ID = 10;
    private static final Integer OTHER_USER_ID = 20;
    private static final Integer FILE_ID = 5;

    @Mock private StoredFileRepository storedFileRepository;

    @InjectMocks private CheckFileAccessUseCase checkFileAccess;
    @InjectMocks private GetFilesByOwnerUseCase getFilesByOwner;

    private static StoredFile file(Integer id, boolean publicCategory) {
        return StoredFile.builder()
                .storedFileId(id)
                .uploadedBy(UPLOADER_ID)
                .deleted(false)
                .fileCategory(FileCategory.builder()
                        .code(publicCategory ? "PROFILE_PICTURE" : "ORGANIZER_DOCUMENT")
                        .isPublic(publicCategory)
                        .build())
                .build();
    }

    @Nested
    @DisplayName("Read (file info and presigned URL)")
    class Read {

        @Test
        @DisplayName("Private document → another user is denied")
        void privateFile_otherUser_isDenied() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file(FILE_ID, false)));

            assertThatThrownBy(() -> checkFileAccess.assertCanRead(FILE_ID, OTHER_USER_ID, false))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Private document → the uploader and an admin are allowed")
        void privateFile_uploaderOrAdmin_isAllowed() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file(FILE_ID, false)));

            assertThatCode(() -> checkFileAccess.assertCanRead(FILE_ID, UPLOADER_ID, false)).doesNotThrowAnyException();
            assertThatCode(() -> checkFileAccess.assertCanRead(FILE_ID, OTHER_USER_ID, true)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Public category → any authenticated user is allowed")
        void publicFile_anyUser_isAllowed() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file(FILE_ID, true)));

            assertThatCode(() -> checkFileAccess.assertCanRead(FILE_ID, OTHER_USER_ID, false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Missing file → NotFoundException")
        void missingFile_isNotFound() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> checkFileAccess.assertCanRead(FILE_ID, UPLOADER_ID, false))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete")
    class Delete {

        @Test
        @DisplayName("Public file of someone else → denied (public read does not mean public delete)")
        void publicFile_otherUser_cannotDelete() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file(FILE_ID, true)));

            assertThatThrownBy(() -> checkFileAccess.assertCanDelete(FILE_ID, OTHER_USER_ID, false))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Uploader and admin → allowed")
        void uploaderOrAdmin_canDelete() {
            when(storedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file(FILE_ID, false)));

            assertThatCode(() -> checkFileAccess.assertCanDelete(FILE_ID, UPLOADER_ID, false)).doesNotThrowAnyException();
            assertThatCode(() -> checkFileAccess.assertCanDelete(FILE_ID, OTHER_USER_ID, true)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("List by owner → hides private files the requester cannot see")
    void listByOwner_hidesPrivateFilesOfOthers() {
        when(storedFileRepository.findByOwnerEntity(1, "USER"))
                .thenReturn(List.of(file(1, true), file(2, false)));

        assertThat(getFilesByOwner.getByOwner(1, "USER", OTHER_USER_ID, false))
                .extracting("storedFileId").containsExactly(1);
        assertThat(getFilesByOwner.getByOwner(1, "USER", UPLOADER_ID, false))
                .extracting("storedFileId").containsExactly(1, 2);
    }
}
