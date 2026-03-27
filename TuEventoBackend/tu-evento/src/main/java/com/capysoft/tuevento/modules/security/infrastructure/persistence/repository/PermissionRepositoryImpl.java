package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.PermissionMapper;
import com.capysoft.tuevento.modules.security.domain.model.Permission;
import com.capysoft.tuevento.modules.security.domain.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final PermissionMapper mapper;

    @Override
    public Permission save(Permission permission) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(permission)));
    }

    @Override
    public Optional<Permission> findById(Integer permissionId) {
        return jpaRepository.findById(permissionId).map(mapper::toDomain);
    }

    @Override
    public Optional<Permission> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Permission> findByRoleId(Integer roleId) {
        return jpaRepository.findByRoleId(roleId).stream().map(mapper::toDomain).toList();
    }
}
