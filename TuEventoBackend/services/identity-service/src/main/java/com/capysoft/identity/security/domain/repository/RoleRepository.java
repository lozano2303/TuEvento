package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Role save(Role role);
    Optional<Role> findById(Integer roleId);
    Optional<Role> findByCode(String code);
    List<Role> findAll();
}
