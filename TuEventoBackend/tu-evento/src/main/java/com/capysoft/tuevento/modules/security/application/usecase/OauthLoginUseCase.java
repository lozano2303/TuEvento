package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.in.OauthLoginPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.domain.event.UserRegisteredEvent;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.model.OauthAccount;
import com.capysoft.tuevento.modules.security.domain.model.RefreshToken;
import com.capysoft.tuevento.modules.security.domain.model.Role;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.repository.OauthAccountRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.AliasGenerator;
import com.capysoft.tuevento.shared.domain.valueobject.ValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthLoginUseCase implements OauthLoginPort {

    private static final int    ACCESS_TOKEN_MINUTES = 15;
    private static final int    REFRESH_TOKEN_DAYS   = 7;
    private static final String DEFAULT_ROLE_CODE    = "USER";
    private static final String DEFAULT_STATUS_CODE  = "ACTIVE";

    private final OauthAccountRepository     oauthAccountRepository;
    private final UserRepository             userRepository;
    private final RoleRepository             roleRepository;
    private final UserStatusRepository       userStatusRepository;
    private final AuthSessionRepository      authSessionRepository;
    private final RefreshTokenRepository     refreshTokenRepository;
    private final LoginCredentialsRepository loginCredentialsRepository;
    private final TokenGeneratorPort         tokenGenerator;
    private final ApplicationEventPublisher  eventPublisher;
    private final CreateProfilePort          createProfilePort;
    private final ProfileRepository          profileRepository;

    /** Registry of provider-specific profile resolvers — extensible without modifying this class. */
    private final Map<String, Function<String, OauthProfile>> providerResolvers;

    // ── Code-based OAuth flow (redirect / callback) ───────────────────────────

    @Override
    @Transactional
    public LoginResponse login(String provider, String code) {
        Function<String, OauthProfile> resolver = providerResolvers.get(provider.toLowerCase());
        if (resolver == null) {
            throw new BusinessException("UNSUPPORTED_PROVIDER", "OAuth provider not supported");
        }
        return loginWithProfile(provider, resolver.apply(code));
    }

    // ── Profile-based flow (GSI id_token already verified server-side) ────────

    @Override
    @Transactional
    public LoginResponse loginWithProfile(String provider, OauthProfile profile) {
        Optional<OauthAccount> existing = oauthAccountRepository
                .findByProviderAndProviderUserId(provider.toLowerCase(), profile.getProviderUserId());

        User user;
        boolean isNewUser         = false;
        boolean isNeedsOnboarding = false;
        Long    existingProfileId = null;   // populated when a profile row already exists

        if (existing.isPresent()) {
            user = existing.get().getUser();

            // ── Check whether the stored fullName is still valid ──────────────
            // Covers users that were created before the fix (their profile was
            // auto-filled with the email prefix, e.g. "crislozanoshark2006").
            // We only evaluate the profile that already exists; we never create
            // one here — that stays exclusively in the new-user branch below.
            Optional<Profile> existingProfile =
                    profileRepository.findByUserId(user.getUserId());

            if (existingProfile.isPresent()) {
                existingProfileId = existingProfile.get().getProfileId();
                if (!ValidationUtils.isValidFullName(existingProfile.get().getFullName())) {
                    log.warn("Existing OAuth user {} has invalid fullName '{}' — triggering onboarding",
                            user.getUserId(), existingProfile.get().getFullName());
                    isNeedsOnboarding = true;
                }
            } else {
                // Profile row is missing entirely (edge case: user was created
                // before profile creation was added to the OAuth flow).
                log.warn("Existing OAuth user {} has no profile row — triggering onboarding",
                        user.getUserId());
                isNeedsOnboarding = true;
            }
        } else {
            // If the email matches a local account, auto-link the OAuth provider
            // instead of rejecting — email is guaranteed verified by the caller
            // (GoogleIdTokenAuthUseCase rejects unverified emails before reaching here).
            Optional<LoginCredentials> localCredentials =
                    (profile.getEmail() != null && !profile.getEmail().isBlank())
                            ? loginCredentialsRepository.findByEmail(profile.getEmail())
                            : Optional.empty();

            if (localCredentials.isPresent()) {
                // Account exists locally — link the OAuth provider and return JWT.
                // Idempotent: findByProviderAndProviderUserId already checked above,
                // so this branch only runs when the oauth_account row does not yet exist.
                user = localCredentials.get().getUser();

                // Apply the same synthetic email fallback used in the new-user branch:
                // Facebook may not return an email when the user hasn't granted the
                // permission, so we must not store null in the oauth_account.email column.
                String linkedEmail = (profile.getEmail() != null && !profile.getEmail().isBlank())
                        ? profile.getEmail()
                        : profile.getProviderUserId() + "@" + provider.toLowerCase() + ".oauth";

                oauthAccountRepository.save(OauthAccount.builder()
                        .user(user)
                        .provider(provider.toLowerCase())
                        .providerUserId(profile.getProviderUserId())
                        .email(linkedEmail)
                        .linkedAt(LocalDateTime.now())
                        .build());

                // Fall through to token generation below — isNewUser stays false.

            } else {
            // No local account and no existing OAuth account → register new user.

            Role role = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Default role not found"));
            UserStatus status = userStatusRepository.findByCode(DEFAULT_STATUS_CODE)
                    .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "Default status not found"));

            String alias = AliasGenerator.generateUnique(
                    profile.getEmail(), profile.getAlias(), userRepository::existsByAlias);

            // Facebook with public_profile scope may not return email — synthetic fallback
            String email = (profile.getEmail() != null && !profile.getEmail().isBlank())
                    ? profile.getEmail()
                    : profile.getProviderUserId() + "@" + provider.toLowerCase() + ".oauth";

            user = userRepository.save(User.builder()
                    .role(role)
                    .userStatus(status)
                    .alias(alias)
                    .activated(true)
                    .build());

            oauthAccountRepository.save(OauthAccount.builder()
                    .user(user)
                    .provider(provider.toLowerCase())
                    .providerUserId(profile.getProviderUserId())
                    .email(email)
                    .linkedAt(LocalDateTime.now())
                    .build());

            // Create the user profile if Google returned a valid display name.
            // If the name is missing or doesn't pass the two-word letter-only rule
            // (e.g. email-prefix fallbacks like "crislozanoshark2006"), we skip
            // profile creation and set needsOnboarding=true so the frontend can
            // redirect the user to a profile-completion step.
            if (ValidationUtils.isValidFullName(profile.getFullName())) {
                try {
                    var createdProfile = createProfilePort.create(CreateProfileRequest.builder()
                            .userId(user.getUserId())
                            .fullName(profile.getFullName().trim())
                            .build());
                    existingProfileId = createdProfile.getProfileId();
                } catch (Exception ex) {
                    // Log but do not fail login — the user can complete their profile later.
                    log.warn("Could not create profile for new OAuth user {}: {}", user.getUserId(), ex.getMessage());
                    isNeedsOnboarding = true;
                }
            } else {
                log.info("OAuth provider '{}' returned name '{}' that did not pass validation — user {} will be prompted for onboarding",
                        provider, profile.getFullName(), user.getUserId());
                isNeedsOnboarding = true;
            }

            isNewUser = true;
            } // end else (new user)
        }

        String accessToken  = tokenGenerator.generateAccessToken(
                user.getUserId(), user.getAlias(), user.getRole().getCode());
        String refreshToken = tokenGenerator.generateRefreshToken(user.getUserId());
        LocalDateTime now   = LocalDateTime.now();

        AuthSession session = authSessionRepository.save(AuthSession.builder()
                .user(user)
                .accessToken(accessToken)
                .issuedAt(now)
                .expiresAt(now.plusMinutes(ACCESS_TOKEN_MINUTES))
                .revoked(false)
                .build());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .authSession(session)
                .token(refreshToken)
                .issuedAt(now)
                .expiresAt(now.plusDays(REFRESH_TOKEN_DAYS))
                .revoked(false)
                .build());

        if (isNewUser) {
            eventPublisher.publishEvent(UserRegisteredEvent.builder()
                    .userId(user.getUserId())
                    .alias(user.getAlias())
                    .email(profile.getEmail() != null ? profile.getEmail() : "")
                    .occurredAt(now)
                    .build());
        }

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .alias(user.getAlias())
                .role(user.getRole().getCode())
                .needsOnboarding(isNeedsOnboarding)
                .profileId(existingProfileId)
                .build();
    }
}
