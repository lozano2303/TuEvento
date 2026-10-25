package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.OauthAccount;
import com.capysoft.tuevento.modules.security.domain.model.Role;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.OauthAccountRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Google (GSI) and Facebook both end in loginWithProfile: an existing account must pass the
 * same account checks as the password login before any token or session is issued.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OauthLoginUseCase — account status")
class OauthLoginUseCaseAccountStatusTest {

    private static final String PROVIDER = "facebook";
    private static final String FB_ID    = "fb-123";
    private static final String EMAIL    = "ana.perez@gmail.com";

    @Mock OauthAccountRepository     oauthAccountRepository;
    @Mock UserRepository             userRepository;
    @Mock RoleRepository             roleRepository;
    @Mock UserStatusRepository       userStatusRepository;
    @Mock AuthSessionRepository      authSessionRepository;
    @Mock RefreshTokenRepository     refreshTokenRepository;
    @Mock LoginCredentialsRepository loginCredentialsRepository;
    @Mock TokenGeneratorPort         tokenGenerator;
    @Mock ApplicationEventPublisher  eventPublisher;
    @Mock CreateProfilePort          createProfilePort;
    @Mock ProfileRepository          profileRepository;
    @Mock AccountAccessGuard         accountAccessGuard;

    private OauthLoginUseCase useCase;
    private User existingUser;
    private OauthProfile profile;

    @BeforeEach
    void setUp() {
        useCase = new OauthLoginUseCase(oauthAccountRepository, userRepository, roleRepository,
                userStatusRepository, authSessionRepository, refreshTokenRepository,
                loginCredentialsRepository, tokenGenerator, eventPublisher, createProfilePort,
                profileRepository, accountAccessGuard, Map.of());

        existingUser = User.builder()
                .userId(5)
                .alias("anaperez")
                .role(Role.builder().code("USER").build())
                .userStatus(UserStatus.builder().code("BLOCKED").build())
                .activated(true)
                .build();

        profile = OauthProfile.builder()
                .providerUserId(FB_ID)
                .email(EMAIL)
                .fullName("Ana Perez")
                .alias("Ana Perez")
                .build();
    }

    private void givenLinkedOauthAccount() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, FB_ID))
                .thenReturn(Optional.of(OauthAccount.builder().user(existingUser).build()));
    }

    private void assertNothingIssued() {
        verify(tokenGenerator, never()).generateAccessToken(any(), any(), any());
        verify(authSessionRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("linked OAuth account that is blocked → rejected before any token is issued")
    void linkedAccount_blocked_rejected() {
        givenLinkedOauthAccount();
        doThrow(new BusinessException("ACCOUNT_BLOCKED", "blocked"))
                .when(accountAccessGuard).assertCanSignIn(existingUser);

        assertThatThrownBy(() -> useCase.loginWithProfile(PROVIDER, profile))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ACCOUNT_BLOCKED");
        assertNothingIssued();
    }

    @Test
    @DisplayName("local account found by email that is deactivated → rejected and the provider is not linked")
    void emailMatch_deactivated_rejectedAndNotLinked() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, FB_ID)).thenReturn(Optional.empty());
        when(loginCredentialsRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(LoginCredentials.builder().user(existingUser).email(EMAIL).build()));
        doThrow(new BusinessException("ACCOUNT_DEACTIVATED", "deactivated"))
                .when(accountAccessGuard).assertCanSignIn(existingUser);

        assertThatThrownBy(() -> useCase.loginWithProfile(PROVIDER, profile))
                .hasFieldOrPropertyWithValue("code", "ACCOUNT_DEACTIVATED");
        verify(oauthAccountRepository, never()).save(any());
        assertNothingIssued();
    }

    @Test
    @DisplayName("linked OAuth account that passes the checks → signs in")
    void linkedAccount_allowed_signsIn() {
        givenLinkedOauthAccount();
        when(profileRepository.findByUserId(5)).thenReturn(Optional.empty());
        when(tokenGenerator.generateAccessToken(5, "anaperez", "USER")).thenReturn("access");
        when(tokenGenerator.generateRefreshToken(5)).thenReturn("refresh");
        when(authSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, AuthSession.class));

        LoginResponse response = useCase.loginWithProfile(PROVIDER, profile);

        verify(accountAccessGuard).assertCanSignIn(existingUser);
        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getUserId()).isEqualTo(5);
    }
}
