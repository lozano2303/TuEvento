package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.dto.request.LogoutRequest;
import com.capysoft.tuevento.modules.security.application.port.in.LogoutPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogoutUseCase implements LogoutPort {

    private final AuthSessionRepository  authSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenGeneratorPort     tokenGenerator;
    private final TokenRevocationPort    tokenRevocationPort;

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        AuthSession session = authSessionRepository.findByAccessToken(request.getAccessToken())
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found"));

        // Verify the session belongs to the authenticated user
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (!session.getUser().getUserId().equals(securityUser.getUserId())) {
            throw new BusinessException("UNAUTHORIZED_LOGOUT", "Cannot logout another user's session");
        }

        session.setRevoked(true);
        session.setRevokedAt(LocalDateTime.now());
        authSessionRepository.save(session);

        refreshTokenRepository.findByAuthSessionId(session.getAuthSessionId())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    rt.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(rt);
                });

        // El access token de esta sesión deja de valer ya, no cuando expire (las demás sesiones siguen).
        // Si la lista de revocación no responde, el logout se deshace (fail-closed).
        tokenGenerator.extractMetadata(request.getAccessToken())
                .ifPresent(m -> tokenRevocationPort.revokeToken(m.tokenId(), m.expiresAt()));
    }
}
