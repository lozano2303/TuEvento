package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByAuthSessionId(Integer authSessionId);
    void revokeAllByUserId(Integer userId);
}
