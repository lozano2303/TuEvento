package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    Permission save(Permission permission);
    Optional<Permission> findById(Integer permissionId);
    Optional<Permission> findByCode(String code);
    List<Permission> findAll();
    List<Permission> findByRoleId(Integer roleId);
}
