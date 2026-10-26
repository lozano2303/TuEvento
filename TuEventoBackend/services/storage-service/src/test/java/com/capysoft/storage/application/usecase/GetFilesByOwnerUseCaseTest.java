package com.capysoft.storage.application.usecase;

import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.StoredFileResponse;
import com.capysoft.storage.application.port.out.StorageClientPort;
import com.capysoft.storage.application.service.StorageAccessPolicy;
import com.capysoft.storage.application.service.StoredFileResponseFactory;
import com.capysoft.storage.domain.model.StoredFile;
import com.capysoft.storage.domain.repository.StoredFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.capysoft.storage.StorageTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetFilesByOwnerUseCase}: the listing only returns files the requester can read.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GetFilesByOwnerUseCase")
class GetFilesByOwnerUseCaseTest {

    @Mock private StoredFileRepository storedFileRepository;
    @Mock private StorageClientPort    storageClient;

    private GetFilesByOwnerUseCase useCase;

    @BeforeEach
    void setUp() {
        StorageAccessPolicy policy = new StorageAccessPolicy();
        StoredFileResponseFactory factory = new StoredFileResponseFactory(storageClient, policy);
        ReflectionTestUtils.setField(factory, "defaultBucket", "tuevento");
        useCase = new GetFilesByOwnerUseCase(storedFileRepository, policy, factory);

        StoredFile document = organizerDocument();
        document.setStoredFileId(11);
        when(storedFileRepository.findByOwnerEntity(OWNER_ID, "USER"))
                .thenReturn(List.of(profilePicture(), document));
        when(storageClient.generatePublicUrl(anyString(), anyString())).thenReturn("https://s3.example.com/x");
    }

    private List<Integer> idsFor(StorageRequester requester) {
        return useCase.getByOwner(OWNER_ID, "USER", requester).stream()
                .map(StoredFileResponse::getStoredFileId)
                .toList();
    }

    @Test
    @DisplayName("anonymous → only public files")
    void anonymous_onlyPublic() {
        assertThat(idsFor(ANONYMOUS)).containsExactly(FILE_ID);
    }

    @Test
    @DisplayName("owner → public files and their private files")
    void owner_all() {
        assertThat(idsFor(OWNER)).containsExactly(FILE_ID, 11);
    }

    @Test
    @DisplayName("other user → only public files")
    void other_onlyPublic() {
        assertThat(idsFor(OTHER)).containsExactly(FILE_ID);
    }

    @Test
    @DisplayName("ADMIN → everything")
    void admin_all() {
        assertThat(idsFor(ADMIN)).containsExactly(FILE_ID, 11);
    }
}
