package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.AuthSession;

import java.util.List;
import java.util.Optional;

public interface AuthSessionRepository {

    AuthSession save(AuthSession authSession);
    Optional<AuthSession> findById(Integer authSessionId);
    Optional<AuthSession> findByAccessToken(String accessToken);
    List<AuthSession> findActiveByUserId(Integer userId);
    void revokeAllByUserId(Integer userId);
}
