package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.LoginCredentialsMapper;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LoginCredentialsRepositoryImpl implements LoginCredentialsRepository {

    private final LoginCredentialsJpaRepository jpaRepository;
    private final LoginCredentialsMapper mapper;

    @Override
    public LoginCredentials save(LoginCredentials loginCredentials) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(loginCredentials)));
    }

    @Override
    public Optional<LoginCredentials> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<LoginCredentials> findByUserId(Integer userId) {
        return jpaRepository.findByUserUserId(userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
