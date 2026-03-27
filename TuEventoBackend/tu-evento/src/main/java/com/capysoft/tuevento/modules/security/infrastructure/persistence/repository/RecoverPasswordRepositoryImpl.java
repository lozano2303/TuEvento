package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.RecoverPasswordMapper;
import com.capysoft.tuevento.modules.security.domain.model.RecoverPassword;
import com.capysoft.tuevento.modules.security.domain.repository.RecoverPasswordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecoverPasswordRepositoryImpl implements RecoverPasswordRepository {

    private final RecoverPasswordJpaRepository jpaRepository;
    private final RecoverPasswordMapper mapper;

    @Override
    public RecoverPassword save(RecoverPassword recoverPassword) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(recoverPassword)));
    }

    @Override
    public Optional<RecoverPassword> findActiveByUserId(Integer userId) {
        return jpaRepository.findActiveByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<RecoverPassword> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public void invalidateAllByUserId(Integer userId) {
        jpaRepository.invalidateAllByUserId(userId);
    }
}
