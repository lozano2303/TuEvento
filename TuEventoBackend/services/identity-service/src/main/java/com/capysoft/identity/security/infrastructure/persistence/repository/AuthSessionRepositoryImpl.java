package com.capysoft.identity.security.infrastructure.persistence.repository;

import com.capysoft.identity.security.application.mapper.AuthSessionMapper;
import com.capysoft.identity.security.domain.model.AuthSession;
import com.capysoft.identity.security.domain.repository.AuthSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthSessionRepositoryImpl implements AuthSessionRepository {

    private final AuthSessionJpaRepository jpaRepository;
    private final AuthSessionMapper mapper;

    @Override
    public AuthSession save(AuthSession authSession) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(authSession)));
    }

    @Override
    public Optional<AuthSession> findById(Integer authSessionId) {
        return jpaRepository.findById(authSessionId).map(mapper::toDomain);
    }

    @Override
    public Optional<AuthSession> findByAccessToken(String accessToken) {
        return jpaRepository.findFirstByAccessTokenOrderByAuthSessionIdDesc(accessToken).map(mapper::toDomain);
    }

    @Override
    public List<AuthSession> findActiveByUserId(Integer userId) {
        return jpaRepository.findActiveByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void revokeAllByUserId(Integer userId) {
        jpaRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }
}
