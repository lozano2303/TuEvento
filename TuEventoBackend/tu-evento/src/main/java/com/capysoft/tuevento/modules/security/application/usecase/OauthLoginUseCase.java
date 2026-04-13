package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.capysoft.tuevento.modules.security.domain.repository.OauthAccountRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.AliasGenerator;

import lombok.RequiredArgsConstructor;

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

    /** Registry of provider-specific profile resolvers — extensible without modifying this class. */
    private final Map<String, Function<String, OauthProfile>> providerResolvers;

    @Override
    @Transactional
    public LoginResponse login(String provider, String code) {
        Function<String, OauthProfile> resolver = providerResolvers.get(provider.toLowerCase());
        if (resolver == null) {
            throw new BusinessException("UNSUPPORTED_PROVIDER", "OAuth provider not supported");
        }

        OauthProfile profile = resolver.apply(code);

        Optional<OauthAccount> existing = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, profile.getProviderUserId());

        User user;
        boolean isNewUser = false;

        if (existing.isPresent()) {
            user = existing.get().getUser();
        } else {
            // Prevent duplicate account if email already exists as a local account
            if (profile.getEmail() != null && !profile.getEmail().isBlank()
                    && loginCredentialsRepository.existsByEmail(profile.getEmail())) {
                throw new BusinessException("EMAIL_ALREADY_EXISTS_AS_LOCAL",
                        "This email is already registered with a password. Please login with email and password");
            }

            Role role = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Default role not found"));
            UserStatus status = userStatusRepository.findByCode(DEFAULT_STATUS_CODE)
                    .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "Default status not found"));

            String alias = AliasGenerator.generateUnique(
                    profile.getEmail(), profile.getAlias(), userRepository::existsByAlias);

            // Facebook with public_profile scope doesn't return email — use a synthetic one
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
                    .provider(provider)
                    .providerUserId(profile.getProviderUserId())
                    .email(email)
                    .linkedAt(LocalDateTime.now())
                    .build());

            isNewUser = true;
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
                .build();
    }
}
