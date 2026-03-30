package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.AccountLockoutMapper;
import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountLockoutRepositoryImpl implements AccountLockoutRepository {

    private final AccountLockoutJpaRepository jpaRepository;
    private final AccountLockoutMapper mapper;

    @Override
    public AccountLockout save(AccountLockout accountLockout) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(accountLockout)));
    }

    @Override
    public Optional<AccountLockout> findByUserId(Integer userId) {
        return jpaRepository.findByUserUserId(userId).map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(Integer userId) {
        jpaRepository.deleteByUserUserId(userId);
    }
}
