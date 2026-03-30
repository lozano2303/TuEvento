package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.AccountActivationMapper;
import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.repository.AccountActivationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountActivationRepositoryImpl implements AccountActivationRepository {

    private final AccountActivationJpaRepository jpaRepository;
    private final AccountActivationMapper mapper;

    @Override
    public AccountActivation save(AccountActivation accountActivation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(accountActivation)));
    }

    @Override
    public Optional<AccountActivation> findByUserId(Integer userId) {
        return jpaRepository.findByUserUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<AccountActivation> findByActivationCode(String activationCode) {
        return jpaRepository.findByActivationCode(activationCode).map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(Integer userId) {
        jpaRepository.deleteByUserUserId(userId);
    }
}
