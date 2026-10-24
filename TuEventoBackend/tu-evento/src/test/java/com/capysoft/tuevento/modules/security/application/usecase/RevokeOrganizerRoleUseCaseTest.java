package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RoleEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit tests for {@link RevokeOrganizerRoleUseCase}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>Happy path: ORGANIZER user is downgraded to USER and event is published</li>
 *   <li>User not found → NotFoundException</li>
 *   <li>User is not an ORGANIZER → BusinessException</li>
 *   <li>USER role not found in DB → NotFoundException</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RevokeOrganizerRoleUseCase")
class RevokeOrganizerRoleUseCaseTest {

    @Mock private UserJpaRepository         userJpaRepository;
    @Mock private RoleJpaRepository         roleJpaRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RevokeOrganizerRoleUseCase useCase;

    private static final Integer USER_ID = 10;

    private RoleEntity organizerRole;
    private RoleEntity userRole;
    private UserEntity organizerUser;

    @BeforeEach
    void setUp() {
        organizerRole = RoleEntity.builder().roleId(2).code("ORGANIZER").name("Organizer").build();
        userRole      = RoleEntity.builder().roleId(1).code("USER").name("User").build();
        organizerUser = UserEntity.builder()
                .userId(USER_ID)
                .alias("john_organizer")
                .role(organizerRole)
                .activated(true)
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("revoke() downgrades ORGANIZER to USER and saves the entity")
        void revoke_organizerUser_savesWithUserRole() {
            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.revoke(USER_ID);

            assertThat(organizerUser.getRole().getCode()).isEqualTo("USER");
            verify(userJpaRepository).save(organizerUser);
        }

        @Test
        @DisplayName("revoke() publishes OrganizerRoleRevokedEvent with correct userId and alias")
        void revoke_publishesOrganizerRoleRevokedEvent() {
            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.revoke(USER_ID);

            ArgumentCaptor<OrganizerRoleRevokedEvent> captor =
                    ArgumentCaptor.forClass(OrganizerRoleRevokedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            OrganizerRoleRevokedEvent event = captor.getValue();
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getAlias()).isEqualTo("john_organizer");
            assertThat(event.getOccurredAt()).isNotNull();
        }
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error paths")
    class ErrorPaths {

        @Test
        @DisplayName("user not found → throws NotFoundException with USER_NOT_FOUND code")
        void revoke_userNotFound_throwsNotFoundException() {
            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.revoke(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("user does not hold ORGANIZER role → throws BusinessException")
        void revoke_userNotOrganizer_throwsBusinessException() {
            UserEntity regularUser = UserEntity.builder()
                    .userId(USER_ID)
                    .alias("regular_user")
                    .role(userRole)
                    .activated(true)
                    .build();

            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> useCase.revoke(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not have the ORGANIZER role");

            verify(roleJpaRepository, never()).findByCode(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("user has null role → treated as non-ORGANIZER, throws BusinessException")
        void revoke_userNullRole_throwsBusinessException() {
            UserEntity noRoleUser = UserEntity.builder()
                    .userId(USER_ID)
                    .alias("no_role_user")
                    .role(null)
                    .activated(true)
                    .build();

            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.of(noRoleUser));

            assertThatThrownBy(() -> useCase.revoke(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not have the ORGANIZER role");

            verify(roleJpaRepository, never()).findByCode(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("USER role not found in DB → throws NotFoundException")
        void revoke_userRoleNotFound_throwsNotFoundException() {
            when(userJpaRepository.findByIdEager(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.revoke(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("USER role not found");

            verify(userJpaRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
