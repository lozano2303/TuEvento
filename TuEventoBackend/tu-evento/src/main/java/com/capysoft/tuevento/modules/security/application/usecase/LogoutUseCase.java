package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.LogoutRequest;
import com.capysoft.tuevento.modules.security.application.port.in.LogoutPort;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogoutUseCase implements LogoutPort {

    private final AuthSessionRepository  authSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        AuthSession session = authSessionRepository.findByAccessToken(request.getAccessToken())
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found"));

        session.setRevoked(true);
        session.setRevokedAt(LocalDateTime.now());
        authSessionRepository.save(session);

        refreshTokenRepository.findByAuthSessionId(session.getAuthSessionId())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    rt.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(rt);
                });
    }
}
