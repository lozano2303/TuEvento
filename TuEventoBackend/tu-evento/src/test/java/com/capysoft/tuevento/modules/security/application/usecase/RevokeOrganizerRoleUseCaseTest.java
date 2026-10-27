package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.OrganizerRoleRevokedMessage;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort.TokenRevocationUnavailableException;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerRoleRevocationRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RoleEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxEventPublisher;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 *   <li>Reason shorter than 10 chars → BusinessException, nothing changes</li>
 *   <li>OrganizerRoleRevoked goes to the outbox with one stable eventId per revocation</li>
 *   <li>Catálogo's events are not touched here (CloseRevokedOrganizerEventsUseCase does it)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RevokeOrganizerRoleUseCase")
class RevokeOrganizerRoleUseCaseTest {

    @Mock private UserJpaRepository         userJpaRepository;
    @Mock private RoleJpaRepository         roleJpaRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OrganizerRoleRevocationRepository revocationRepository;
    @Mock private OutboxEventPublisher      outboxEventPublisher;
    @Mock private TokenRevocationPort       tokenRevocationPort;

    private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");

    private RevokeOrganizerRoleUseCase useCase;

    private static final Integer USER_ID  = 10;
    private static final Integer ADMIN_ID = 1;
    private static final String  REASON   = "Incumplimiento de las normas de la plataforma";

    private RoleEntity organizerRole;
    private RoleEntity userRole;
    private UserEntity organizerUser;

    @BeforeEach
    void setUp() {
        useCase = new RevokeOrganizerRoleUseCase(userJpaRepository, roleJpaRepository, eventPublisher,
                revocationRepository, outboxEventPublisher, tokenRevocationPort, Clock.fixed(NOW, ZoneOffset.UTC));
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
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.revoke(USER_ID, REASON, ADMIN_ID);

            assertThat(organizerUser.getRole().getCode()).isEqualTo("USER");
            verify(userJpaRepository).save(organizerUser);
        }

        @Test
        @DisplayName("revoke() publishes OrganizerRoleRevokedEvent with correct userId and alias")
        void revoke_publishesOrganizerRoleRevokedEvent() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.revoke(USER_ID, REASON, ADMIN_ID);

            ArgumentCaptor<OrganizerRoleRevokedEvent> captor =
                    ArgumentCaptor.forClass(OrganizerRoleRevokedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            OrganizerRoleRevokedEvent event = captor.getValue();
            assertThat(event.getUserId()).isEqualTo(USER_ID);
            assertThat(event.getAlias()).isEqualTo("john_organizer");
            assertThat(event.getOccurredAt()).isNotNull();
            assertThat(event.getReason()).isEqualTo(REASON);
            assertThat(event.getRevokedBy()).isEqualTo(ADMIN_ID);
            verify(revocationRepository).record(USER_ID, ADMIN_ID, REASON);
        }

        @Test
        @DisplayName("revoke() deja OrganizerRoleRevoked en el outbox con un eventId estable por revocación")
        void revoke_publishesToOutbox() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(revocationRepository.record(USER_ID, ADMIN_ID, REASON)).thenReturn(77L);

            useCase.revoke(USER_ID, REASON, ADMIN_ID);

            ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<UUID> eventId = ArgumentCaptor.forClass(UUID.class);
            verify(outboxEventPublisher).publish(eq("user"), eq("10"), eq("OrganizerRoleRevoked"),
                    payload.capture(), eventId.capture());
            assertThat(payload.getValue()).isEqualTo(new OrganizerRoleRevokedMessage(
                    77L, USER_ID, "john_organizer", ADMIN_ID, REASON, "2026-10-27T15:00:00Z"));
            assertThat(eventId.getValue())
                    .isEqualTo(RevokeOrganizerRoleUseCase.eventIdOf(77L))
                    .isNotEqualTo(RevokeOrganizerRoleUseCase.eventIdOf(78L));
        }

        @Test
        @DisplayName("revoke() invalida los access tokens del usuario emitidos hasta ahora")
        void revoke_revokesUserTokens() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.revoke(USER_ID, REASON, ADMIN_ID);

            verify(tokenRevocationPort).revokeUser(USER_ID, NOW);
        }

        @Test
        @DisplayName("revoke() falla entera si la lista de revocación no responde: no queda a medias")
        void revoke_failsWhenRevocationStoreIsDown() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new TokenRevocationUnavailableException("redis down", null))
                    .when(tokenRevocationPort).revokeUser(USER_ID, NOW);

            assertThatThrownBy(() -> useCase.revoke(USER_ID, REASON, ADMIN_ID))
                    .isInstanceOf(TokenRevocationUnavailableException.class);
            verifyNoInteractions(outboxEventPublisher, eventPublisher);
        }

        @Test
        @DisplayName("revoke() lee al usuario con bloqueo de fila: dos revocaciones simultáneas no pasan ambas")
        void revoke_locksTheUser() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
            when(userJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.revoke(USER_ID, REASON, ADMIN_ID);

            verify(userJpaRepository).findByIdEagerForUpdate(USER_ID);
            verify(userJpaRepository, never()).findByIdEager(any());
        }
    }

    // ── Error paths ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error paths")
    class ErrorPaths {

        @Test
        @DisplayName("user not found → throws NotFoundException with USER_NOT_FOUND code")
        void revoke_userNotFound_throwsNotFoundException() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.revoke(USER_ID, REASON, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(eventPublisher, never()).publishEvent(any());
            verifyNoInteractions(outboxEventPublisher);
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

            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> useCase.revoke(USER_ID, REASON, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not have the ORGANIZER role");

            verify(roleJpaRepository, never()).findByCode(any());
            verify(eventPublisher, never()).publishEvent(any());
            verifyNoInteractions(outboxEventPublisher);
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

            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(noRoleUser));

            assertThatThrownBy(() -> useCase.revoke(USER_ID, REASON, ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("does not have the ORGANIZER role");

            verify(roleJpaRepository, never()).findByCode(any());
            verify(eventPublisher, never()).publishEvent(any());
            verifyNoInteractions(outboxEventPublisher);
        }

        @Test
        @DisplayName("USER role not found in DB → throws NotFoundException")
        void revoke_userRoleNotFound_throwsNotFoundException() {
            when(userJpaRepository.findByIdEagerForUpdate(USER_ID)).thenReturn(Optional.of(organizerUser));
            when(roleJpaRepository.findByCode("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.revoke(USER_ID, REASON, ADMIN_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("USER role not found");

            verify(userJpaRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
            verifyNoInteractions(outboxEventPublisher);
        }

        @Test
        @DisplayName("reason shorter than 10 chars → throws BusinessException, nothing changes")
        void revoke_shortReason_throwsBusinessException() {
            assertThatThrownBy(() -> useCase.revoke(USER_ID, "corto", ADMIN_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reason");

            verifyNoInteractions(userJpaRepository, revocationRepository, outboxEventPublisher, eventPublisher);
        }
    }
}
