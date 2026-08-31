package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.AccountReactivationTokenMapper;
import com.capysoft.tuevento.modules.security.domain.model.AccountReactivationToken;
import com.capysoft.tuevento.modules.security.domain.repository.AccountReactivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountReactivationTokenRepositoryImpl implements AccountReactivationTokenRepository {

    private final AccountReactivationTokenJpaRepository jpaRepository;
    private final AccountReactivationTokenMapper        mapper;

    @Override
    public AccountReactivationToken save(AccountReactivationToken token) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<AccountReactivationToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public void invalidateAllByUserId(Integer userId) {
        jpaRepository.invalidateAllByUserId(userId);
    }
}
