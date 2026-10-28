package com.capysoft.identity.security.application.usecase;

import com.capysoft.identity.security.domain.event.OrganizerApprovedEvent;
import com.capysoft.identity.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import com.capysoft.identity.security.infrastructure.persistence.entity.RoleEntity;
import com.capysoft.identity.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.identity.security.infrastructure.persistence.repository.OrganizerPetitionJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApproveOrganizerRequestUseCase}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: PENDING petition is approved, user is promoted to ORGANIZER and event is published</li>
 *   <li>Happy path: user without a previous role is still promoted</li>
 *   <li>Petition not found → NotFoundException (PETITION_NOT_FOUND)</li>
 *   <li>Petition not PENDING (APPROVED / REJECTED) → BusinessException (PETITION_NOT_PENDING)</li>
 *   <li>ORGANIZER role missing in DB → NotFoundException (ROLE_NOT_FOUND)</li>
 *   <li>Petition user missing → NotFoundException (USER_NOT_FOUND)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApproveOrganizerRequestUseCase")
class ApproveOrganizerRequestUseCaseTest {

    @Mock private OrganizerPetitionJpaRepository jpaRepository;
    @Mock private UserJpaRepository              userJpaRepository;
    @Mock private RoleJpaRepository              roleJpaRepository;
    @Mock private ApplicationEventPublisher      eventPublisher;

    @InjectMocks
    private ApproveOrganizerRequestUseCase useCase;

    private static final Integer PETITION_ID = 7;
    private static final Integer USER_ID     = 21;

    private RoleEntity              userRole;
    private RoleEntity              organizerRole;
    private UserEntity              applicant;
    private OrganizerPetitionEntity petition;

    @BeforeEach
    void setUp() {
        userRole      = RoleEntity.builder().roleId(1).code("USER").name("User").build();
        organizerRole = RoleEntity.builder().roleId(2).code("ORGANIZER").name("Organizer").build();
        applicant = UserEntity.builder()
                .userId(USER_ID)
                .alias("maria_applicant")
                .role(userRole)
                .activated(true)
                .build();
        petition = OrganizerPetitionEntity.builder()
                .organizerPetitionId(PETITION_ID)
                .user(applicant)
                .status("PENDING")
                .build();
    }

    private void stubHappyPath() {
        when(jpaRepository.findByIdEager(PETITION_ID)).thenReturn(Optional.of(petition));
        when(roleJpaRepository.findByCode("ORGANIZER")).thenReturn(Optional.of(organizerRole));
        when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.of(applicant));
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("approve() marks the petition as APPROVED and saves it")
        void approve_pendingPetition_savesAsApproved() {
            stubHappyPath();

            useCase.approve(PETITION_ID);

            assertThat(petition.getStatus()).isEqualTo("APPROVED");
            verify(jpaRepository).save(petition);
        }

        @Test
        @DisplayName("approve() promotes the applicant from USER to ORGANIZER")
        void approve_pendingPetition_promotesUserToOrganizer() {
            stubHappyPath();

            useCase.approve(PETITION_ID);

            assertThat(applicant.getRole().getCode()).isEqualTo("ORGANIZER");
            verify(userJpaRepository).save(applicant);
        }

        @Test
        @DisplayName("approve() promotes an applicant that had no role assigned")
        void approve_applicantWithoutRole_promotesToOrganizer() {
            applicant.setRole(null);
            stubHappyPath();

            useCase.approve(PETITION_ID);

            assertThat(applicant.getRole().getCode()).isEqualTo("ORGANIZER");
            verify(userJpaRepository).save(applicant);
        }

        @Test
        @DisplayName("approve() publishes OrganizerApprovedEvent with petition id, user id and alias")
        void approve_pendingPetition_publishesOrganizerApprovedEvent() {
            stubHappyPath();

            useCase.approve(PETITION_ID);

            ArgumentCaptor<OrganizerApprovedEvent> captor = ArgumentCaptor.forClass(OrganizerApprovedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            OrganizerApprovedEvent event = captor.getValue();
            assertThat(event.getOrganizerPetitionId()).isEqualTo(PETITION_ID);
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getAlias()).isEqualTo("maria_applicant");
            assertThat(event.getOccurredAt()).isNotNull();
        }
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error paths")
    class ErrorPaths {

        @Test
        @DisplayName("petition not found → throws NotFoundException with PETITION_NOT_FOUND code")
        void approve_petitionNotFound_throwsNotFoundException() {
            when(jpaRepository.findByIdEager(PETITION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.approve(PETITION_ID))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("code").isEqualTo("PETITION_NOT_FOUND");

            verify(jpaRepository, never()).save(any());
            verifyNoInteractions(roleJpaRepository, userJpaRepository, eventPublisher);
        }

        @ParameterizedTest(name = "status {0} → PETITION_NOT_PENDING")
        @ValueSource(strings = {"APPROVED", "REJECTED"})
        @DisplayName("petition already resolved → throws BusinessException and leaves the role untouched")
        void approve_petitionNotPending_throwsBusinessException(String status) {
            petition.setStatus(status);
            when(jpaRepository.findByIdEager(PETITION_ID)).thenReturn(Optional.of(petition));

            assertThatThrownBy(() -> useCase.approve(PETITION_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("PETITION_NOT_PENDING");

            assertThat(petition.getStatus()).isEqualTo(status);
            assertThat(applicant.getRole().getCode()).isEqualTo("USER");
            verify(jpaRepository, never()).save(any());
            verifyNoInteractions(roleJpaRepository, userJpaRepository, eventPublisher);
        }

        @Test
        @DisplayName("ORGANIZER role missing in DB → throws NotFoundException with ROLE_NOT_FOUND code")
        void approve_organizerRoleMissing_throwsNotFoundException() {
            when(jpaRepository.findByIdEager(PETITION_ID)).thenReturn(Optional.of(petition));
            when(roleJpaRepository.findByCode("ORGANIZER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.approve(PETITION_ID))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("code").isEqualTo("ROLE_NOT_FOUND");

            verify(userJpaRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("petition user missing → throws NotFoundException with USER_NOT_FOUND code")
        void approve_userNotFound_throwsNotFoundException() {
            when(jpaRepository.findByIdEager(PETITION_ID)).thenReturn(Optional.of(petition));
            when(roleJpaRepository.findByCode("ORGANIZER")).thenReturn(Optional.of(organizerRole));
            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.approve(PETITION_ID))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("code").isEqualTo("USER_NOT_FOUND");

            verify(userJpaRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
