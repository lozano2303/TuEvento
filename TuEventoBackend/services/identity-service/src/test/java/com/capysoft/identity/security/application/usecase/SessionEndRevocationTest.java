package com.capysoft.identity.security.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.capysoft.identity.security.application.dto.request.DeactivateAccountRequest;
import com.capysoft.identity.security.application.dto.request.LogoutRequest;
import com.capysoft.identity.security.application.port.out.PasswordEncoderPort;
import com.capysoft.identity.security.application.port.out.TokenGeneratorPort;
import com.capysoft.identity.security.application.port.out.TokenGeneratorPort.TokenMetadata;
import com.capysoft.identity.security.application.port.out.TokenRevocationPort;
import com.capysoft.identity.security.application.port.out.TokenRevocationPort.TokenRevocationUnavailableException;
import com.capysoft.identity.security.domain.model.AuthSession;
import com.capysoft.identity.security.domain.model.LoginCredentials;
import com.capysoft.identity.security.domain.model.User;
import com.capysoft.identity.security.domain.model.UserStatus;
import com.capysoft.identity.security.domain.repository.AuthSessionRepository;
import com.capysoft.identity.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.identity.security.domain.repository.RefreshTokenRepository;
import com.capysoft.identity.security.domain.repository.UserRepository;
import com.capysoft.identity.security.domain.repository.UserStatusHistoryRepository;
import com.capysoft.identity.security.domain.repository.UserStatusRepository;
import com.capysoft.common.infrastructure.security.SecurityUser;

/** Cerrar sesión y desactivar la cuenta invalidan también los access tokens ya emitidos. */
@DisplayName("Revocación de access tokens al terminar sesiones")
class SessionEndRevocationTest {

    private static final Integer USER_ID = 15;

    @BeforeEach
    void authenticate() {
        SecurityUser principal = new SecurityUser(USER_ID, "bob", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static User user() {
        return User.builder().userId(USER_ID).userStatus(UserStatus.builder().code("ACTIVE").build()).build();
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("logout")
    class Logout {

        @Mock AuthSessionRepository authSessionRepository;
        @Mock RefreshTokenRepository refreshTokenRepository;
        @Mock TokenGeneratorPort tokenGenerator;
        @Mock TokenRevocationPort tokenRevocationPort;
        @InjectMocks LogoutUseCase useCase;

        private final Instant expiresAt = Instant.parse("2026-10-27T15:15:00Z");

        @BeforeEach
        void session() {
            when(authSessionRepository.findByAccessToken("acc"))
                    .thenReturn(Optional.of(AuthSession.builder().authSessionId(3).user(user()).build()));
            when(refreshTokenRepository.findByAuthSessionId(3)).thenReturn(Optional.empty());
            when(tokenGenerator.extractMetadata("acc"))
                    .thenReturn(Optional.of(new TokenMetadata("jti-1", Instant.parse("2026-10-27T15:00:00Z"), expiresAt)));
        }

        @Test
        @DisplayName("revoca el access token de esa sesión hasta que expire, y solo ese")
        void revokesThisAccessToken() {
            useCase.logout(LogoutRequest.builder().accessToken("acc").build());

            verify(tokenRevocationPort).revokeToken("jti-1", expiresAt);
            verify(tokenRevocationPort, never()).revokeUser(anyInt(), any());
        }

        @Test
        @DisplayName("si la lista de revocación no responde, el logout falla (no se da por cerrado)")
        void failsWhenStoreIsDown() {
            doThrow(new TokenRevocationUnavailableException("down", null))
                    .when(tokenRevocationPort).revokeToken(any(), any());

            assertThatThrownBy(() -> useCase.logout(LogoutRequest.builder().accessToken("acc").build()))
                    .isInstanceOf(TokenRevocationUnavailableException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("desactivar la cuenta")
    class Deactivate {

        @Mock LoginCredentialsRepository loginCredentialsRepository;
        @Mock UserRepository userRepository;
        @Mock UserStatusRepository userStatusRepository;
        @Mock UserStatusHistoryRepository userStatusHistoryRepository;
        @Mock AuthSessionRepository authSessionRepository;
        @Mock RefreshTokenRepository refreshTokenRepository;
        @Mock PasswordEncoderPort passwordEncoder;
        @Mock TokenRevocationPort tokenRevocationPort;
        @InjectMocks DeactivateAccountUseCase useCase;

        @Test
        @DisplayName("revoca todos los access tokens del usuario emitidos hasta ese momento")
        void revokesAllUserTokens() {
            when(loginCredentialsRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(LoginCredentials.builder().user(user()).passwordHash("hash").build()));
            when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
            when(userStatusRepository.findByCode("INACTIVE"))
                    .thenReturn(Optional.of(UserStatus.builder().code("INACTIVE").build()));
            when(authSessionRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());
            Instant before = Instant.now();

            useCase.deactivate(DeactivateAccountRequest.builder().password("secret").build());

            ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
            verify(tokenRevocationPort).revokeUser(eq(USER_ID), cutoff.capture());
            assertThat(cutoff.getValue()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("con la contraseña incorrecta no revoca nada")
        void wrongPassword_revokesNothing() {
            when(loginCredentialsRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(LoginCredentials.builder().user(user()).passwordHash("hash").build()));
            when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

            assertThatThrownBy(() -> useCase.deactivate(DeactivateAccountRequest.builder().password("bad").build()));
            verify(tokenRevocationPort, never()).revokeUser(anyInt(), any());
        }
    }
}
