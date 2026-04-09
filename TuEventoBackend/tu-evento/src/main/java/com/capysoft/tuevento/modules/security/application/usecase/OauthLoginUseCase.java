package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.in.OauthLoginPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.domain.event.UserRegisteredEvent;
import com.capysoft.tuevento.modules.security.domain.model.*;
import com.capysoft.tuevento.modules.security.domain.repository.*;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.AliasGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class OauthLoginUseCase implements OauthLoginPort {

    private static final int ACCESS_TOKEN_MINUTES = 15;
    private static final int REFRESH_TOKEN_DAYS   = 7;
    private static final String DEFAULT_ROLE_CODE  = "USER";
    private static final String DEFAULT_STATUS_CODE = "ACTIVE";

    private final OauthAccountRepository  oauthAccountRepository;
    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final UserStatusRepository    userStatusRepository;
    private final AuthSessionRepository   authSessionRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final TokenGeneratorPort      tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    /** Registry of provider-specific profile resolvers — extensible without modifying this class. */
    private final Map<String, Function<String, OauthProfile>> providerResolvers;

    @Override
    @Transactional
    public LoginResponse login(String provider, String code) {
        Function<String, OauthProfile> resolver = providerResolvers.get(provider.toLowerCase());
        if (resolver == null) {
            throw new BusinessException("UNSUPPORTED_PROVIDER", "OAuth provider not supported: " + provider);
        }

        OauthProfile profile = resolver.apply(code);

        Optional<OauthAccount> existing = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, profile.getProviderUserId());

        User user;
        boolean isNewUser = false;

        if (existing.isPresent()) {
            user = existing.get().getUser();
        } else {
            Role role = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Default role not found"));
            UserStatus status = userStatusRepository.findByCode(DEFAULT_STATUS_CODE)
                    .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "Default status not found"));

            String alias = AliasGenerator.generateUnique(profile.getEmail(), userRepository::existsByAlias);

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
                    .email(profile.getEmail())
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
                    .email(profile.getEmail())
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
