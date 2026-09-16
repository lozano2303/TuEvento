package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OauthLoginUseCase focused on the onboarding detection logic:
 * existing OAuth users whose profile.fullName fails isValidFullName() must receive
 * needsOnboarding=true so the frontend redirects them to /complete-profile.
 *
 * These tests document the regression path discovered in the Google OAuth name fix
 * (crislozanoshark2006 bug) and guard against future regressions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OauthLoginUseCase — onboarding detection for existing OAuth users")
class OauthLoginUseCaseOnboardingTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

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

    @InjectMocks
    OauthLoginUseCase useCase;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private static final String PROVIDER      = "google";
    private static final String PROVIDER_SUB  = "google-sub-123";
    private static final String USER_EMAIL    = "crislozanoshark2006@gmail.com";
    private static final Integer USER_ID      = 42;
    private static final Long    PROFILE_ID   = 7L;

    private User          stubUser;
    private OauthAccount  stubOauthAccount;
    private OauthProfile  incomingProfile;

    @BeforeEach
    void setUp() {
        // Wire the providerResolvers map — OauthLoginUseCase requires it via
        // @RequiredArgsConstructor but for the GSI flow tested here it is unused.
        // Mockito @InjectMocks won't inject Map<>, so we set it manually via
        // a tiny subclass trick: we re-create the use case with the map injected.
        useCase = new OauthLoginUseCase(
                oauthAccountRepository,
                userRepository,
                roleRepository,
                userStatusRepository,
                authSessionRepository,
                refreshTokenRepository,
                loginCredentialsRepository,
                tokenGenerator,
                eventPublisher,
                createProfilePort,
                profileRepository,
                Map.of()   // no provider resolvers needed for loginWithProfile()
        );

        stubUser = User.builder()
                .userId(USER_ID)
                .alias("crislozanoshark2006")
                .role(Role.builder().code("USER").build())
                .activated(true)
                .build();

        stubOauthAccount = OauthAccount.builder()
                .user(stubUser)
                .provider(PROVIDER)
                .providerUserId(PROVIDER_SUB)
                .email(USER_EMAIL)
                .build();

        incomingProfile = OauthProfile.builder()
                .providerUserId(PROVIDER_SUB)
                .email(USER_EMAIL)
                .fullName("Cristian Lozano")   // Google now returns the real name
                .alias("Cristian Lozano")
                .build();

        // Token stubs — always return deterministic strings
        when(tokenGenerator.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("access-token-stub");
        when(tokenGenerator.generateRefreshToken(any()))
                .thenReturn("refresh-token-stub");

        // Session/refresh persistence — return minimal stubs
        when(authSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("existing OAuth user with invalid fullName (email prefix) → needsOnboarding=true + profileId set")
    void existingUser_invalidFullName_triggersOnboarding() {
        // Existing OAuth account found
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_SUB))
                .thenReturn(Optional.of(stubOauthAccount));

        // Profile exists but name is the legacy email-prefix garbage
        Profile badProfile = Profile.builder()
                .profileId(PROFILE_ID)
                .userId(USER_ID)
                .fullName("crislozanoshark2006")   // ← invalid: digits, no spaces
                .build();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(badProfile));

        LoginResponse response = useCase.loginWithProfile(PROVIDER, incomingProfile);

        assertThat(response.isNeedsOnboarding()).isTrue();
        assertThat(response.getProfileId()).isEqualTo(PROFILE_ID);

        // createProfile must NOT be called — onboarding redirect is the correct path
        verify(createProfilePort, never()).create(any());
    }

    @Test
    @DisplayName("existing OAuth user with valid fullName → needsOnboarding=false, no redirect")
    void existingUser_validFullName_noOnboarding() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_SUB))
                .thenReturn(Optional.of(stubOauthAccount));

        Profile goodProfile = Profile.builder()
                .profileId(PROFILE_ID)
                .userId(USER_ID)
                .fullName("Cristian Lozano")   // ← valid: two words, letters only
                .build();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(goodProfile));

        LoginResponse response = useCase.loginWithProfile(PROVIDER, incomingProfile);

        assertThat(response.isNeedsOnboarding()).isFalse();
        verify(createProfilePort, never()).create(any());
    }

    @Test
    @DisplayName("existing OAuth user with NO profile row at all → needsOnboarding=true, profileId null")
    void existingUser_missingProfile_triggersOnboarding() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_SUB))
                .thenReturn(Optional.of(stubOauthAccount));

        // No profile row — edge case: user created before profile creation was added to OAuth flow
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        LoginResponse response = useCase.loginWithProfile(PROVIDER, incomingProfile);

        assertThat(response.isNeedsOnboarding()).isTrue();
        assertThat(response.getProfileId()).isNull();
        verify(createProfilePort, never()).create(any());
    }

    @Test
    @DisplayName("existing OAuth user — needsOnboarding=false should still return valid tokens")
    void existingUser_validFullName_returnsTokens() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_SUB))
                .thenReturn(Optional.of(stubOauthAccount));

        Profile goodProfile = Profile.builder()
                .profileId(PROFILE_ID)
                .userId(USER_ID)
                .fullName("Cristian Lozano")
                .build();
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(goodProfile));

        LoginResponse response = useCase.loginWithProfile(PROVIDER, incomingProfile);

        assertThat(response.getAccessToken()).isEqualTo("access-token-stub");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-stub");
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getAlias()).isEqualTo(stubUser.getAlias());
    }

    @Test
    @DisplayName("new user — Google name 'crislozanoshark2006' (invalid) → needsOnboarding=true, no profile created")
    void newUser_invalidGoogleName_noProfileCreated() {
        // No existing OAuth account
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_SUB))
                .thenReturn(Optional.empty());
        // No local account either
        when(loginCredentialsRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        Role role = Role.builder().code("USER").build();
        UserStatus status = UserStatus.builder().code("ACTIVE").build();
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(role));
        when(userStatusRepository.findByCode("ACTIVE")).thenReturn(Optional.of(status));
        when(userRepository.existsByAlias(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(stubUser);
        when(oauthAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OauthProfile badNameProfile = OauthProfile.builder()
                .providerUserId(PROVIDER_SUB)
                .email(USER_EMAIL)
                .fullName("crislozanoshark2006")  // ← invalid name from Google
                .alias("crislozanoshark2006")
                .build();

        LoginResponse response = useCase.loginWithProfile(PROVIDER, badNameProfile);

        assertThat(response.isNeedsOnboarding()).isTrue();
        verify(createProfilePort, never()).create(any());
    }

    @Test
    @DisplayName("new user — Google name 'Cristian Lozano' (valid) → needsOnboarding=false, profile created")
    void newUser_validGoogleName_profileCreated() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, PROVIDER_SUB))
                .thenReturn(Optional.empty());
        when(loginCredentialsRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        Role role = Role.builder().code("USER").build();
        UserStatus status = UserStatus.builder().code("ACTIVE").build();
        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(role));
        when(userStatusRepository.findByCode("ACTIVE")).thenReturn(Optional.of(status));
        when(userRepository.existsByAlias(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(stubUser);
        when(oauthAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(createProfilePort.create(any())).thenReturn(
                ProfileResponse.builder().profileId(PROFILE_ID).userId(USER_ID).build());

        LoginResponse response = useCase.loginWithProfile(PROVIDER, incomingProfile);

        assertThat(response.isNeedsOnboarding()).isFalse();
        assertThat(response.getProfileId()).isEqualTo(PROFILE_ID);
        verify(createProfilePort).create(any());
    }
}
