package com.capysoft.identity.security.application.usecase;

import com.capysoft.identity.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.identity.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.identity.security.application.dto.response.OrganizerRequestResponse;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort.DocumentUrl;
import com.capysoft.identity.security.domain.model.OrganizerPetition;
import com.capysoft.identity.security.domain.model.User;
import com.capysoft.identity.security.domain.repository.OrganizerPetitionRepository;
import com.capysoft.identity.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import com.capysoft.identity.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrganizerRequestsUseCase")
class GetOrganizerRequestsUseCaseTest {

    private static final Integer USER_ID = 21;
    private static final Integer ADMIN_ID = 1;
    private static final Integer AVATAR_ID = 40;

    @Mock private OrganizerPetitionRepository organizerPetitionRepository;
    @Mock private LoginCredentialsJpaRepository loginCredentialsRepository;
    @Mock private ProfileJpaRepository profileRepository;
    @Mock private OrganizerDocumentPort organizerDocumentPort;

    @InjectMocks
    private GetOrganizerRequestsUseCase useCase;

    private void pendingPetition() {
        OrganizerPetition petition = OrganizerPetition.builder()
                .organizerPetitionId(5).user(User.builder().userId(USER_ID).build())
                .storedFileId(99).status("PENDING").build();
        when(organizerPetitionRepository.findByStatus("PENDING")).thenReturn(List.of(petition));
        when(loginCredentialsRepository.findByUserUserId(USER_ID))
                .thenReturn(Optional.of(LoginCredentialsEntity.builder().email("ana@example.com").build()));
        when(profileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(ProfileEntity.builder().fullName("Ana").storedFileId(AVATAR_ID).build()));
    }

    @Test
    @DisplayName("resolves the applicant's avatar through the storage port")
    void resolvesAvatarUrl() {
        pendingPetition();
        when(organizerDocumentPort.publicUrl(AVATAR_ID, ADMIN_ID, "ADMIN"))
                .thenReturn(new DocumentUrl(AVATAR_ID, "https://files.example.com/avatar.png"));

        List<OrganizerRequestResponse> requests = useCase.getRequests("pending", ADMIN_ID);

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.getEmail()).isEqualTo("ana@example.com");
            assertThat(request.getFullName()).isEqualTo("Ana");
            assertThat(request.getStoredFileId()).isEqualTo(99);
            assertThat(request.getProfilePicture()).isEqualTo("https://files.example.com/avatar.png");
        });
    }

    @Test
    @DisplayName("a missing avatar file does not break the listing")
    void missingAvatarDegradesGracefully() {
        pendingPetition();
        when(organizerDocumentPort.publicUrl(AVATAR_ID, ADMIN_ID, "ADMIN"))
                .thenThrow(new IllegalStateException("file not found"));

        assertThat(useCase.getRequests("pending", ADMIN_ID))
                .singleElement()
                .satisfies(request -> assertThat(request.getProfilePicture()).isNull());
    }
}
