package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.RoleMapper;
import com.capysoft.tuevento.modules.security.domain.model.Role;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final RoleMapper mapper;

    @Override
    public Role save(Role role) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(role)));
    }

    @Override
    public Optional<Role> findById(Integer roleId) {
        return jpaRepository.findById(roleId).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
