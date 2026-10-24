package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.security.application.dto.response.OrganizerUserResponse;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RoleEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetOrganizersUseCase}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Empty list returned when no organizers exist</li>
 *   <li>Full data mapped correctly — email, fullName, counts, presigned URL</li>
 *   <li>Missing profile → response with null fullName / null profilePicture</li>
 *   <li>Avatar storedFileId is null → profilePicture is null (no URL generation attempt)</li>
 *   <li>GeneratePublicUrlPort throws → profilePicture is null (resilient)</li>
 *   <li>loginCredentialsRepository throws → email is null (resilient)</li>
 *   <li>profileRepository throws → fullName / profilePicture are null (resilient)</li>
 *   <li>event count queries throw → counts default to zero (resilient)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrganizersUseCase")
class GetOrganizersUseCaseTest {

    @Mock private UserJpaRepository             userJpaRepository;
    @Mock private LoginCredentialsJpaRepository loginCredentialsRepository;
    @Mock private ProfileJpaRepository          profileRepository;
    @Mock private GeneratePublicUrlPort         generatePublicUrlPort;

    @InjectMocks
    private GetOrganizersUseCase useCase;

    private static final Integer USER_ID = 5;
    private RoleEntity organizerRole;
    private UserEntity organizerUser;

    @BeforeEach
    void setUp() {
        organizerRole = RoleEntity.builder().roleId(2).code("ORGANIZER").name("Organizer").build();
        organizerUser = UserEntity.builder()
                .userId(USER_ID)
                .alias("org_alice")
                .role(organizerRole)
                .activated(true)
                .build();
    }

    // ── Empty list ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns empty list when there are no organizer users")
    void getOrganizers_noOrganizers_returnsEmptyList() {
        when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of());

        List<OrganizerUserResponse> result = useCase.getOrganizers();

        assertThat(result).isEmpty();
        verifyNoInteractions(loginCredentialsRepository, profileRepository, generatePublicUrlPort);
    }

    // ── Full data mapping ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Full data mapping")
    class FullDataMapping {

        @Test
        @DisplayName("maps all fields correctly when all data is available")
        void getOrganizers_fullData_mapsCorrectly() {
            ProfileEntity profile = ProfileEntity.builder()
                    .userId(USER_ID)
                    .fullName("Alice Smith")
                    .storedFileId(42)
                    .build();

            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID)).thenReturn(Optional.of("alice@example.com"));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(generatePublicUrlPort.generate(42))
                    .thenReturn(PublicUrlResponse.builder().publicUrl("https://cdn.example.com/avatar.jpg").build());
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenReturn(10L);
            when(userJpaRepository.countPublishedEventsByUserId((long) USER_ID)).thenReturn(7L);

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result).hasSize(1);
            OrganizerUserResponse r = result.get(0);
            assertThat(r.getUserId()).isEqualTo(USER_ID);
            assertThat(r.getAlias()).isEqualTo("org_alice");
            assertThat(r.getEmail()).isEqualTo("alice@example.com");
            assertThat(r.getFullName()).isEqualTo("Alice Smith");
            assertThat(r.getProfilePicture()).isEqualTo("https://cdn.example.com/avatar.jpg");
            assertThat(r.getTotalEvents()).isEqualTo(10L);
            assertThat(r.getPublishedEvents()).isEqualTo(7L);
        }

        @Test
        @DisplayName("profilePicture is null when storedFileId is null")
        void getOrganizers_noAvatar_profilePictureNull() {
            ProfileEntity profileNoAvatar = ProfileEntity.builder()
                    .userId(USER_ID)
                    .fullName("Alice Smith")
                    .storedFileId(null)
                    .build();

            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID)).thenReturn(Optional.empty());
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profileNoAvatar));
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenReturn(0L);
            when(userJpaRepository.countPublishedEventsByUserId((long) USER_ID)).thenReturn(0L);

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result.get(0).getProfilePicture()).isNull();
            verify(generatePublicUrlPort, never()).generate(any());
        }
    }

    // ── Resilience ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resilience — partial failures return null fields instead of throwing")
    class Resilience {

        @Test
        @DisplayName("missing profile row → fullName and profilePicture are null, response still returned")
        void getOrganizers_missingProfile_nullFields() {
            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID)).thenReturn(Optional.of("alice@example.com"));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenReturn(3L);
            when(userJpaRepository.countPublishedEventsByUserId((long) USER_ID)).thenReturn(1L);

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result).hasSize(1);
            OrganizerUserResponse r = result.get(0);
            assertThat(r.getFullName()).isNull();
            assertThat(r.getProfilePicture()).isNull();
            assertThat(r.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("GeneratePublicUrlPort throws → profilePicture is null, method does not re-throw")
        void getOrganizers_urlGenerationFails_profilePictureNull() {
            ProfileEntity profile = ProfileEntity.builder()
                    .userId(USER_ID)
                    .fullName("Alice Smith")
                    .storedFileId(99)
                    .build();

            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID)).thenReturn(Optional.empty());
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(generatePublicUrlPort.generate(99)).thenThrow(new RuntimeException("S3 error"));
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenReturn(0L);
            when(userJpaRepository.countPublishedEventsByUserId((long) USER_ID)).thenReturn(0L);

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProfilePicture()).isNull();
        }

        @Test
        @DisplayName("loginCredentialsRepository throws → email is null, method does not re-throw")
        void getOrganizers_emailQueryFails_emailNull() {
            ProfileEntity profile = ProfileEntity.builder()
                    .userId(USER_ID)
                    .fullName("Alice Smith")
                    .storedFileId(null)
                    .build();

            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID))
                    .thenThrow(new RuntimeException("DB error"));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenReturn(0L);
            when(userJpaRepository.countPublishedEventsByUserId((long) USER_ID)).thenReturn(0L);

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isNull();
        }

        @Test
        @DisplayName("profileRepository throws → fullName and profilePicture are null, other fields kept")
        void getOrganizers_profileQueryFails_nullProfileFields() {
            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID)).thenReturn(Optional.of("alice@mail.com"));
            when(profileRepository.findByUserId(USER_ID)).thenThrow(new RuntimeException("DB error"));
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenReturn(4L);
            when(userJpaRepository.countPublishedEventsByUserId((long) USER_ID)).thenReturn(1L);

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result).hasSize(1);
            OrganizerUserResponse r = result.get(0);
            assertThat(r.getFullName()).isNull();
            assertThat(r.getProfilePicture()).isNull();
            assertThat(r.getEmail()).isEqualTo("alice@mail.com");
            assertThat(r.getTotalEvents()).isEqualTo(4L);
            verifyNoInteractions(generatePublicUrlPort);
        }

        @Test
        @DisplayName("event count queries throw → counts default to zero")
        void getOrganizers_countQueryFails_countsZero() {
            when(userJpaRepository.findAllByRoleCode("ORGANIZER")).thenReturn(List.of(organizerUser));
            when(loginCredentialsRepository.findEmailByUserId(USER_ID)).thenReturn(Optional.empty());
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(userJpaRepository.countEventsByUserId((long) USER_ID)).thenThrow(new RuntimeException("DB error"));

            List<OrganizerUserResponse> result = useCase.getOrganizers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTotalEvents()).isZero();
            assertThat(result.get(0).getPublishedEvents()).isZero();
            assertThat(result.get(0).getAlias()).isEqualTo("org_alice");
        }
    }
}
