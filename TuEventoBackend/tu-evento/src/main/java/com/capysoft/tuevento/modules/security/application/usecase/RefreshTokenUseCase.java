package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.dto.request.RefreshTokenRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RefreshTokenResponse;
import com.capysoft.tuevento.modules.security.application.port.in.RefreshTokenPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.model.RefreshToken;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase implements RefreshTokenPort {

    private static final int ACCESS_TOKEN_MINUTES = 15;
    private static final int REFRESH_TOKEN_DAYS   = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionRepository  authSessionRepository;
    private final TokenGeneratorPort     tokenGenerator;

    @Override
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken oldRefresh = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new NotFoundException("REFRESH_TOKEN_NOT_FOUND", "Refresh token not found"));

        if (oldRefresh.getRevoked()) {
            throw new BusinessException("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked");
        }
        if (oldRefresh.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        User user = oldRefresh.getUser();

        // Verify the user account is still active
        if (!user.getActivated()) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }
        String statusCode = user.getUserStatus().getCode();
        if (!"ACTIVE".equals(statusCode)) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVE", "Account is not active");
        }

        LocalDateTime now = LocalDateTime.now();

        oldRefresh.setRevoked(true);
        oldRefresh.setRevokedAt(now);
        refreshTokenRepository.save(oldRefresh);

        AuthSession oldSession = oldRefresh.getAuthSession();
        oldSession.setRevoked(true);
        oldSession.setRevokedAt(now);
        authSessionRepository.save(oldSession);

        String newAccessToken  = tokenGenerator.generateAccessToken(
                user.getUserId(), user.getAlias(), user.getRole().getCode());
        String newRefreshToken = tokenGenerator.generateRefreshToken(user.getUserId());

        AuthSession newSession = authSessionRepository.save(AuthSession.builder()
                .user(user)
                .accessToken(newAccessToken)
                .issuedAt(now)
                .expiresAt(now.plusMinutes(ACCESS_TOKEN_MINUTES))
                .revoked(false)
                .build());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .authSession(newSession)
                .token(newRefreshToken)
                .issuedAt(now)
                .expiresAt(now.plusDays(REFRESH_TOKEN_DAYS))
                .revoked(false)
                .build());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
