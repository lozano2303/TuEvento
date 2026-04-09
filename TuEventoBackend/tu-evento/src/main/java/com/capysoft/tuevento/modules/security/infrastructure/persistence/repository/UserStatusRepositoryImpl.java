package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.UserStatusMapper;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserStatusRepositoryImpl implements UserStatusRepository {

    private final UserStatusJpaRepository jpaRepository;
    private final UserStatusMapper mapper;

    @Override
    public Optional<UserStatus> findById(Integer userStatusId) {
        return jpaRepository.findById(userStatusId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserStatus> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<UserStatus> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
