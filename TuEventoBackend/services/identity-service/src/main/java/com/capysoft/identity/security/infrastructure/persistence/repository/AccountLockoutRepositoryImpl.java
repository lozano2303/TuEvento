package com.capysoft.identity.security.infrastructure.persistence.repository;

import com.capysoft.identity.security.application.mapper.AccountLockoutMapper;
import com.capysoft.identity.security.domain.model.AccountLockout;
import com.capysoft.identity.security.domain.repository.AccountLockoutRepository;
import com.capysoft.identity.security.infrastructure.persistence.entity.AccountLockoutEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountLockoutRepositoryImpl implements AccountLockoutRepository {

    private final AccountLockoutJpaRepository jpaRepository;
    private final AccountLockoutMapper        mapper;
    private final UserJpaRepository           userJpaRepository;

    @Override
    public AccountLockout save(AccountLockout accountLockout) {
        AccountLockoutEntity entity = mapper.toEntity(accountLockout);
        entity.setUser(userJpaRepository.getReferenceById(
                accountLockout.getUser().getUserId()));
        AccountLockoutEntity saved = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
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
